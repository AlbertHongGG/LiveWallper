package com.livewallpaper.app.presentation.preview

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livewallpaper.app.data.metadata.VideoMetadataRetriever
import com.livewallpaper.app.domain.model.DownloadProgress
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.VideoSource
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.usecase.ApplyWallpaperUseCase
import com.livewallpaper.app.domain.usecase.ProcessVideoSourceUseCase
import com.livewallpaper.app.domain.usecase.ResetWallpaperUseCase
import com.livewallpaper.app.presentation.notification.NotificationController
import com.livewallpaper.app.service.VideoWallpaperService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    data class NavigateToEditor(val filePath: String) : MainUiEffect
}

class MainViewModel(
    private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val processVideoSourceUseCase: ProcessVideoSourceUseCase,
    private val applyWallpaperUseCase: ApplyWallpaperUseCase,
    private val resetWallpaperUseCase: ResetWallpaperUseCase
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

    private val _effect = MutableSharedFlow<MainUiEffect>()
    val effect: SharedFlow<MainUiEffect> = _effect.asSharedFlow()

    private var currentConfigSnapshot: WallpaperConfig = WallpaperConfig()
    private var downloadJob: Job? = null

    init {
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
        val wallpaperManager = WallpaperManager.getInstance(context)
        val currentInfo = wallpaperManager.wallpaperInfo ?: return false
        val myService = ComponentName(context, VideoWallpaperService::class.java)
        return currentInfo.component == myService
    }

    fun refreshServiceStatus() {
        val running = isWallpaperServiceRunning()
        val current = _stageState.value
        if (current is WallpaperStageState.Active) {
            _stageState.value = current.copy(isServiceActive = running)
        }
    }

    fun updateUrlInput(text: String) {
        _urlInput.value = text
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

    fun handleVideoSource(source: VideoSource, onReadyForEditor: (String) -> Unit) {
        downloadJob?.cancel()
        _downloadState.value = DownloadUiState(
            isDownloading = true,
            status = "準備中..."
        )

        downloadJob = viewModelScope.launch {
            processVideoSourceUseCase(source)
                .catch { exception ->
                    val errorMsg = when (exception) {
                        is WallpaperError.InvalidUrl -> "未偵測到 Pinterest 影片網址"
                        is WallpaperError.UnsupportedContent -> "該貼文不包含影片串流"
                        is WallpaperError.DownloadFailed -> exception.message ?: "下載失敗"
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
                            onReadyForEditor(progress.outputFile.absolutePath)
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

    fun applyWallpaper(config: WallpaperConfig, onFinished: () -> Unit) {
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
                    onFinished()
                } else {
                    _effect.emit(MainUiEffect.RequestSetWallpaperIntent)
                }
            }.onFailure { throwable ->
                NotificationController.showError(throwable.localizedMessage ?: "套用桌布失敗")
            }
        }
    }

    fun getCurrentConfig(): WallpaperConfig = currentConfigSnapshot

    class Factory(
        private val context: Context,
        private val wallpaperRepository: WallpaperRepository,
        private val processVideoSourceUseCase: ProcessVideoSourceUseCase,
        private val applyWallpaperUseCase: ApplyWallpaperUseCase,
        private val resetWallpaperUseCase: ResetWallpaperUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                context,
                wallpaperRepository,
                processVideoSourceUseCase,
                applyWallpaperUseCase,
                resetWallpaperUseCase
            ) as T
        }
    }
}
