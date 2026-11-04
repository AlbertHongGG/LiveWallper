package com.livewallpaper.app.presentation.preview

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livewallpaper.app.data.metadata.VideoMetadataRetriever
import com.livewallpaper.app.domain.model.DownloadProgress
import com.livewallpaper.app.domain.model.ExtractorFailure
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.VideoSource
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.parser.SharedUrlResolver
import com.livewallpaper.app.domain.repository.WallpaperPermissionManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import com.livewallpaper.app.domain.usecase.ApplyWallpaperUseCase
import com.livewallpaper.app.domain.usecase.ProcessVideoSourceUseCase
import com.livewallpaper.app.domain.usecase.ResetWallpaperUseCase
import com.livewallpaper.app.presentation.notification.NotificationController
import com.livewallpaper.app.service.VideoWallpaperService
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * 桌布核心舞台狀態機。
 * 明確劃分「初始空狀態」與「活躍桌布就緒狀態」，杜絕狀態混亂與 Nullable 穿透。
 */
sealed interface WallpaperStageState {
    data object Empty : WallpaperStageState

    data class Active(
        val videoFile: File,
        val metadata: VideoMetadata?,
        val config: WallpaperConfig,
        val isServiceActive: Boolean
    ) : WallpaperStageState
}

/**
 * 下載與解析狀態模型。
 */
data class DownloadUiState(
    val isDownloading: Boolean = false,
    val progress: Float? = null,
    val status: String? = null,
    val speed: String? = null
)

sealed interface MainUiEffect {
    data object RequestSetWallpaperIntent : MainUiEffect
    data object ApplyWallpaperSuccess : MainUiEffect
    data class NavigateToEditor(val filePath: String) : MainUiEffect
}

class MainViewModel(
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperPermissionManager: WallpaperPermissionManager,
    private val wallpaperSystemManager: WallpaperSystemManager,
    private val processVideoSourceUseCase: ProcessVideoSourceUseCase,
    private val applyWallpaperUseCase: ApplyWallpaperUseCase,
    private val resetWallpaperUseCase: ResetWallpaperUseCase,
    private val urlResolver: SharedUrlResolver
) : ViewModel() {

    // 1. 舞台狀態流（完全與暫態打字輸入解耦，杜絕打字時的舞台重組）
    private val _stageState = MutableStateFlow<WallpaperStageState>(WallpaperStageState.Empty)
    val stageState: StateFlow<WallpaperStageState> = _stageState.asStateFlow()

    // 2. 獨立的 URL 輸入狀態流
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    // 3. 獨立的下載進度狀態流
    private val _downloadState = MutableStateFlow(DownloadUiState())
    val downloadState: StateFlow<DownloadUiState> = _downloadState.asStateFlow()

    // 4. 原桌布防護備份引導對話框狀態
    private val _showPermissionPrompt = MutableStateFlow(false)
    val showPermissionPrompt: StateFlow<Boolean> = _showPermissionPrompt.asStateFlow()

    private val _effect = Channel<MainUiEffect>(Channel.BUFFERED)
    val effect: Flow<MainUiEffect> = _effect.receiveAsFlow()

    private var currentConfigSnapshot: WallpaperConfig = WallpaperConfig()
    private var downloadJob: Job? = null
    private var pendingApplyConfig: WallpaperConfig? = null
    private var pendingOnFinished: (() -> Unit)? = null

    init {
        checkAndCaptureSnapshot()

        wallpaperRepository.wallpaperConfig
            .onEach { config ->
                currentConfigSnapshot = config
                val isRunning = isWallpaperServiceRunning()

                if (config.hasVideo) {
                    val file = File(config.videoFilePath)
                    if (file.exists() && file.length() > 0L) {
                        val metadata = VideoMetadataRetriever.retrieve(file)
                        _stageState.value = WallpaperStageState.Active(
                            videoFile = file,
                            metadata = metadata,
                            config = config,
                            isServiceActive = isRunning
                        )
                    } else {
                        _stageState.value = WallpaperStageState.Empty
                    }
                } else {
                    _stageState.value = WallpaperStageState.Empty
                }
            }
            .launchIn(viewModelScope)
    }

    fun isWallpaperServiceRunning(): Boolean {
        return wallpaperSystemManager.isWallpaperServiceActive()
    }

    fun refreshServiceStatus() {
        val running = isWallpaperServiceRunning()
        val current = _stageState.value
        if (current is WallpaperStageState.Active) {
            _stageState.value = current.copy(isServiceActive = running)
        }
    }

    fun updateUrlInput(text: String) {
        _urlInput.value = urlResolver.sanitizeInput(text)
    }

    fun resetWallpaper() {
        viewModelScope.launch {
            resetWallpaperUseCase()
                .onSuccess {
                    _stageState.value = WallpaperStageState.Empty
                    NotificationController.showSuccess("已重設並清空當前動態桌布")
                }
                .onFailure { error ->
                    NotificationController.showError("重設桌布失敗: ${error.localizedMessage}")
                }
        }
    }

    fun handleVideoSource(source: VideoSource) {
        downloadJob?.cancel()
        _downloadState.value = DownloadUiState(
            isDownloading = true,
            status = "準備中..."
        )

        downloadJob = viewModelScope.launch {
            processVideoSourceUseCase(source)
                .catch { exception ->
                    val errorMsg = when (exception) {
                        is WallpaperError.InvalidUrl -> "未偵測到有效的影片網址"
                        is WallpaperError.UnsupportedContent -> "該貼文不包含影片串流"
                        is WallpaperError.DownloadFailed -> when (exception.failure) {
                            ExtractorFailure.EngineOutdated -> "解析引擎需要升級，請稍後重試"
                            ExtractorFailure.EngineUnavailable -> "解析引擎初始化失敗，請重新啟動 App"
                            ExtractorFailure.FormatUnavailable -> "該貼文影片格式受限或無法取得相容串流"
                            ExtractorFailure.ProcessingFailed -> "影片轉碼或合併處理失敗，請稍後重試"
                            ExtractorFailure.NetworkUnavailable -> "網路連線異常，請確認網路後重試"
                            ExtractorFailure.ContentUnsupported -> "該貼文不支援或屬於私密內容"
                            is ExtractorFailure.Unknown -> exception.reason
                        }
                        is WallpaperError.StorageError -> exception.message ?: "儲存空間異常"
                        else -> exception.localizedMessage ?: "處理失敗"
                    }
                    _downloadState.value = DownloadUiState(isDownloading = false)
                    NotificationController.showError(errorMsg)
                }
                .collect { progress ->
                    when (progress) {
                        is DownloadProgress.Preparing -> {
                            _downloadState.value = DownloadUiState(
                                isDownloading = true,
                                status = progress.stageMessage
                            )
                        }

                        is DownloadProgress.SelfHealing -> {
                            _downloadState.value = DownloadUiState(
                                isDownloading = true,
                                status = progress.message
                            )
                            NotificationController.showInfo(progress.message)
                        }

                        is DownloadProgress.Downloading -> {
                            _downloadState.value = DownloadUiState(
                                isDownloading = true,
                                progress = progress.progressPercent,
                                status = "下載中 ${progress.progressPercent.toInt()}%",
                                speed = progress.speedText
                            )
                        }

                        is DownloadProgress.Completed -> {
                            _downloadState.value = DownloadUiState(isDownloading = false)
                            _urlInput.value = ""
                            NotificationController.showSuccess("影片解析載入成功")
                            _effect.send(MainUiEffect.NavigateToEditor(progress.outputFile.absolutePath))
                        }
                    }
                }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadState.value = DownloadUiState(isDownloading = false)
        NotificationController.showInfo("已取消下載")
    }

    fun checkAndCaptureSnapshot() {
        viewModelScope.launch {
            if (!wallpaperSystemManager.isWallpaperServiceActive()) {
                if (wallpaperPermissionManager.hasBackupPermission()) {
                    if (!wallpaperSystemManager.hasSnapshot()) {
                        val result = wallpaperSystemManager.captureCurrentWallpaperSnapshot()
                        if (result.getOrDefault(false)) {
                            NotificationController.showSuccess("原桌布已成功安全備份")
                        }
                    }
                    // 若有暫存的套用請求，自動繼續執行套用流程
                    pendingApplyConfig?.let { pendingConfig ->
                        val onFinished = pendingOnFinished ?: {}
                        pendingApplyConfig = null
                        pendingOnFinished = null
                        executeApplyWallpaper(pendingConfig, onFinished)
                    }
                }
            }
        }
    }

    fun requestPermissionIntent() = wallpaperPermissionManager.createRequestPermissionIntent()

    fun dismissPermissionPrompt(proceedWithoutBackup: Boolean = false) {
        _showPermissionPrompt.value = false
        if (proceedWithoutBackup) {
            NotificationController.showInfo("未開啟備份，清空桌布時將回退至原廠預設桌布")
            pendingApplyConfig?.let { pendingConfig ->
                val onFinished = pendingOnFinished ?: {}
                pendingApplyConfig = null
                pendingOnFinished = null
                executeApplyWallpaper(pendingConfig, onFinished)
            }
        } else {
            pendingApplyConfig = null
            pendingOnFinished = null
        }
    }

    fun applyWallpaper(config: WallpaperConfig, onFinished: () -> Unit) {
        // 若尚未備份且未獲授權且非已由服務託管，則彈出引導
        if (!wallpaperSystemManager.hasSnapshot() &&
            !wallpaperPermissionManager.hasBackupPermission() &&
            !isWallpaperServiceRunning()
        ) {
            pendingApplyConfig = config
            pendingOnFinished = onFinished
            _showPermissionPrompt.value = true
            return
        }

        // 嘗試捕獲快照（若已授權）
        if (!wallpaperSystemManager.hasSnapshot() && wallpaperPermissionManager.hasBackupPermission()) {
            viewModelScope.launch {
                wallpaperSystemManager.captureCurrentWallpaperSnapshot()
            }
        }

        executeApplyWallpaper(config, onFinished)
    }

    private fun executeApplyWallpaper(config: WallpaperConfig, onFinished: () -> Unit) {
        viewModelScope.launch {
            val stagingFile = File(config.videoFilePath)
            val result = applyWallpaperUseCase(
                stagingFile = stagingFile,
                config = config
            )

            result.onSuccess { newConfig ->
                currentConfigSnapshot = newConfig
                val isServiceActive = isWallpaperServiceRunning()

                if (isServiceActive) {
                    NotificationController.showSuccess("桌布設定已即時生效")
                    _effect.send(MainUiEffect.ApplyWallpaperSuccess)
                    onFinished()
                } else {
                    _effect.send(MainUiEffect.RequestSetWallpaperIntent)
                }
            }.onFailure { throwable ->
                NotificationController.showError(throwable.localizedMessage ?: "套用桌布失敗")
            }
        }
    }

    fun notifyApplySuccess() {
        viewModelScope.launch {
            _effect.send(MainUiEffect.ApplyWallpaperSuccess)
        }
    }

    fun getCurrentConfig(): WallpaperConfig = currentConfigSnapshot

    class Factory(
        private val wallpaperRepository: WallpaperRepository,
        private val wallpaperPermissionManager: WallpaperPermissionManager,
        private val wallpaperSystemManager: WallpaperSystemManager,
        private val processVideoSourceUseCase: ProcessVideoSourceUseCase,
        private val applyWallpaperUseCase: ApplyWallpaperUseCase,
        private val resetWallpaperUseCase: ResetWallpaperUseCase,
        private val urlResolver: SharedUrlResolver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                wallpaperRepository,
                wallpaperPermissionManager,
                wallpaperSystemManager,
                processVideoSourceUseCase,
                applyWallpaperUseCase,
                resetWallpaperUseCase,
                urlResolver
            ) as T
        }
    }
}
