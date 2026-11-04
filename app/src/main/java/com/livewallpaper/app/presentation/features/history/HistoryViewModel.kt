package com.livewallpaper.app.presentation.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.model.common.MediaFormatters
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.usecase.ClearWallpaperHistoryUseCase
import com.livewallpaper.app.domain.usecase.DeleteHistoryItemUseCase
import com.livewallpaper.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryItemUiModel(
    val item: WallpaperHistoryItem,
    val isCurrentlyActive: Boolean
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<HistoryItemUiModel> = emptyList(),
    val totalSizeFormatted: String = "0 B",
    val totalCount: Int = 0
)

class HistoryViewModel(
    private val historyRepository: WallpaperHistoryRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val deleteHistoryItemUseCase: DeleteHistoryItemUseCase,
    private val clearWallpaperHistoryUseCase: ClearWallpaperHistoryUseCase
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.historyItems,
        wallpaperRepository.wallpaperConfig
    ) { historyList, currentConfig ->
        val models = historyList.map { item ->
            HistoryItemUiModel(
                item = item,
                isCurrentlyActive = currentConfig.hasVideo && currentConfig.videoFilePath == item.videoFilePath
            )
        }
        val totalBytes = historyList.sumOf { it.fileSizeBytes }
        HistoryUiState(
            isLoading = false,
            items = models,
            totalSizeFormatted = MediaFormatters.formatFileSize(totalBytes),
            totalCount = historyList.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun deleteItem(item: WallpaperHistoryItem) {
        viewModelScope.launch {
            deleteHistoryItemUseCase(item.id, item.videoFilePath)
                .onSuccess {
                    NotificationController.showSuccess("已移除該筆歷史桌布")
                }
                .onFailure { error ->
                    NotificationController.showError("刪除失敗: ${error.localizedMessage}")
                }
        }
    }

    fun clearAll(preserveActive: Boolean = true) {
        viewModelScope.launch {
            clearWallpaperHistoryUseCase(preserveActiveWallpaper = preserveActive)
                .onSuccess {
                    NotificationController.showSuccess("已清空歷史紀錄")
                }
                .onFailure { error ->
                    NotificationController.showError("清空失敗: ${error.localizedMessage}")
                }
        }
    }

    class Factory(
        private val historyRepository: WallpaperHistoryRepository,
        private val wallpaperRepository: WallpaperRepository,
        private val deleteHistoryItemUseCase: DeleteHistoryItemUseCase,
        private val clearWallpaperHistoryUseCase: ClearWallpaperHistoryUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(
                historyRepository,
                wallpaperRepository,
                deleteHistoryItemUseCase,
                clearWallpaperHistoryUseCase
            ) as T
        }
    }
}
