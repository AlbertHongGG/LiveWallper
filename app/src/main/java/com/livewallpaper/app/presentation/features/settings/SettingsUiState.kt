package com.livewallpaper.app.presentation.features.settings

data class SettingsUiState(
    val ytDlpVersion: String = "載入中...",
    val isUpdatingYtDlp: Boolean = false,
    val doubleTapToPause: Boolean = true,
    val hasBackupPermission: Boolean = false,
    val hasSnapshotBackup: Boolean = false,
    val stagingSizeFormatted: String = "0 B",
    val stagingFilesCount: Int = 0,
    val archiveSizeFormatted: String = "0 B",
    val archiveFilesCount: Int = 0
)
