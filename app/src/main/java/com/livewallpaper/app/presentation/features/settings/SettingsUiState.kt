package com.livewallpaper.app.presentation.features.settings

data class SettingsUiState(
    val ytDlpVersion: String = "載入中...",
    val isUpdatingYtDlp: Boolean = false,
    val cacheSizeFormatted: String = "0 B",
    val cacheFilesCount: Int = 0,
    val doubleTapToPause: Boolean = true,
    val hasBackupPermission: Boolean = false,
    val hasSnapshotBackup: Boolean = false
)
