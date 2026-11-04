package com.livewallpaper.app.presentation.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livewallpaper.app.domain.repository.MediaExtractor
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(
    private val wallpaperRepository: WallpaperRepository,
    private val mediaExtractor: MediaExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 監聽桌布配置變更
        wallpaperRepository.wallpaperConfig
            .onEach { config ->
                _uiState.update { current ->
                    current.copy(
                        doubleTapToPause = config.doubleTapToPause,
                        powerSaveDelaySeconds = config.powerSaveDelaySeconds
                    )
                }
            }
            .launchIn(viewModelScope)

        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            // 讀取 yt-dlp 版本
            val version = mediaExtractor.getEngineVersion()
            // 讀取快取大小
            val (bytes, count) = mediaExtractor.getCacheStats()
            val formattedSize = formatFileSize(bytes)

            _uiState.update { current ->
                current.copy(
                    ytDlpVersion = version,
                    cacheSizeFormatted = formattedSize,
                    cacheFilesCount = count
                )
            }
        }
    }

    fun toggleDoubleTapToPause(enabled: Boolean) {
        viewModelScope.launch {
            wallpaperRepository.updateConfig { it.copy(doubleTapToPause = enabled) }
        }
    }

    fun setPowerSaveDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            wallpaperRepository.updateConfig { it.copy(powerSaveDelaySeconds = seconds) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            mediaExtractor.clearCache()
            refreshStats()
            NotificationController.showSuccess("已清空所有下載暫存快取")
        }
    }

    fun updateYtDlpEngine() {
        if (_uiState.value.isUpdatingYtDlp) return
        _uiState.update { it.copy(isUpdatingYtDlp = true) }
        NotificationController.showInfo("正在檢查並更新 YT-DLP 核心引擎...")

        viewModelScope.launch {
            val result = mediaExtractor.updateExtractorEngine()
            _uiState.update { it.copy(isUpdatingYtDlp = false) }

            result.onSuccess { status ->
                refreshStats()
                NotificationController.showSuccess("YT-DLP 核心引擎更新完畢: $status")
            }.onFailure {
                refreshStats()
                NotificationController.showWarning("YT-DLP 核心已是最新或網路連線受限")
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }

    class Factory(
        private val wallpaperRepository: WallpaperRepository,
        private val mediaExtractor: MediaExtractor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(wallpaperRepository, mediaExtractor) as T
        }
    }
}
