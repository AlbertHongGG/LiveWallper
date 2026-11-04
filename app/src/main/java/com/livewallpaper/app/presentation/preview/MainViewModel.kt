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

data class HomeUiState(
    val urlInput: String = "",
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val downloadStatus: String? = null,
    val downloadSpeed: String? = null,
    val activeConfig: WallpaperConfig? = null,
    val activeMetadata: VideoMetadata? = null,
    val isServiceActive: Boolean = false
)

sealed interface MainUiEffect {
    data object RequestSetWallpaperIntent : MainUiEffect
    data class NavigateToEditor(val filePath: String) : MainUiEffect
}

class MainViewModel(
    private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val processVideoSourceUseCase: ProcessVideoSourceUseCase,
    private val applyWallpaperUseCase: ApplyWallpaperUseCase
) : ViewModel() {

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _effect = MutableSharedFlow<MainUiEffect>()
    val effect: SharedFlow<MainUiEffect> = _effect.asSharedFlow()

    private var currentConfigSnapshot: WallpaperConfig = WallpaperConfig()
    private var downloadJob: Job? = null

    init {
        wallpaperRepository.wallpaperConfig
            .onEach { config ->
                currentConfigSnapshot = config
                val isRunning = isWallpaperServiceRunning()

                val metadata = if (config.hasVideo) {
                    val file = File(config.videoFilePath)
                    if (file.exists()) VideoMetadataRetriever.retrieve(file) else null
                } else null

                _homeState.update { current ->
                    current.copy(
                        activeConfig = config,
                        activeMetadata = metadata,
                        isServiceActive = isRunning
                    )
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
        _homeState.update { it.copy(isServiceActive = running) }
    }

    fun updateUrlInput(text: String) {
        _homeState.update { it.copy(urlInput = text) }
    }

    fun handleVideoSource(source: VideoSource, onReadyForEditor: (String) -> Unit) {
        downloadJob?.cancel()
        _homeState.update {
            it.copy(
                isDownloading = true,
                downloadProgress = null,
                downloadStatus = "準備中...",
                downloadSpeed = null
            )
        }

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
                    _homeState.update { it.copy(isDownloading = false) }
                    NotificationController.showError(errorMsg)
                }
                .collect { progress ->
                    when (progress) {
                        is DownloadProgress.Preparing -> {
                            _homeState.update {
                                it.copy(
                                    isDownloading = true,
                                    downloadProgress = null,
                                    downloadStatus = progress.stageMessage
                                )
                            }
                        }

                        is DownloadProgress.Downloading -> {
                            _homeState.update {
                                it.copy(
                                    isDownloading = true,
                                    downloadProgress = progress.progressPercent,
                                    downloadStatus = "下載中 ${progress.progressPercent.toInt()}%",
                                    downloadSpeed = progress.speedText
                                )
                            }
                        }

                        is DownloadProgress.Completed -> {
                            _homeState.update {
                                it.copy(
                                    isDownloading = false,
                                    urlInput = ""
                                )
                            }
                            NotificationController.showSuccess("影片解析載入成功")
                            onReadyForEditor(progress.outputFile.absolutePath)
                        }
                    }
                }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _homeState.update { it.copy(isDownloading = false) }
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
                    NotificationController.showSuccess("桌布設定已即時套用生效")
                    onFinished()
                } else {
                    // 若尚未啟用系統桌布，喚起系統對話框
                    _effect.emit(MainUiEffect.RequestSetWallpaperIntent)
                }
            }.onFailure { throwable ->
                NotificationController.showError(throwable.localizedMessage ?: "套用桌布失敗")
            }
        }
    }

    class Factory(
        private val context: Context,
        private val wallpaperRepository: WallpaperRepository,
        private val processVideoSourceUseCase: ProcessVideoSourceUseCase,
        private val applyWallpaperUseCase: ApplyWallpaperUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                context,
                wallpaperRepository,
                processVideoSourceUseCase,
                applyWallpaperUseCase
            ) as T
        }
    }
}
