package com.livewallpaper.app.presentation.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.livewallpaper.app.domain.repository.WallpaperPermissionManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import com.livewallpaper.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val wallpaperRepository: WallpaperRepository,
    private val extractorEngineManager: ExtractorEngineManager,
    private val wallpaperPermissionManager: WallpaperPermissionManager,
    private val wallpaperSystemManager: WallpaperSystemManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 監聽桌布配置變更
        wallpaperRepository.wallpaperConfig
            .onEach { config ->
                _uiState.update { current ->
                    current.copy(
                        doubleTapToPause = config.doubleTapToPause
                    )
                }
            }
            .launchIn(viewModelScope)

        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            // 讀取 yt-dlp 核心版本、權限、快照
            val version = extractorEngineManager.getEngineVersion()
            val hasPermission = wallpaperPermissionManager.hasBackupPermission()
            val hasSnapshot = wallpaperSystemManager.hasSnapshot()

            _uiState.update { current ->
                current.copy(
                    ytDlpVersion = version,
                    hasBackupPermission = hasPermission,
                    hasSnapshotBackup = hasSnapshot
                )
            }
        }
    }

    fun toggleDoubleTapToPause(enabled: Boolean) {
        viewModelScope.launch {
            wallpaperRepository.updateConfig { it.copy(doubleTapToPause = enabled) }
        }
    }

    fun updateYtDlpEngine() {
        if (_uiState.value.isUpdatingYtDlp) return
        _uiState.update { it.copy(isUpdatingYtDlp = true) }
        NotificationController.showInfo("正在檢查並更新 YT-DLP 核心引擎...")

        viewModelScope.launch {
            val result = extractorEngineManager.updateEngine()
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

    class Factory(
        private val wallpaperRepository: WallpaperRepository,
        private val extractorEngineManager: ExtractorEngineManager,
        private val wallpaperPermissionManager: WallpaperPermissionManager,
        private val wallpaperSystemManager: WallpaperSystemManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                wallpaperRepository,
                extractorEngineManager,
                wallpaperPermissionManager,
                wallpaperSystemManager
            ) as T
        }
    }
}
