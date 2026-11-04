package com.livewallpaper.app.presentation.features.editor.model

import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import java.io.File

/**
 * 編輯器單一事實來源 (Single Source of Truth) 狀態模型。
 */
data class WallpaperEditorUiState(
    val videoFile: File,
    val metadata: VideoMetadata? = null,
    val config: WallpaperConfig,
    val isServiceActive: Boolean = false,
    val currentGroup: EditorSettingGroup = EditorSettingGroup.VISUAL
) {
    val durationMs: Long
        get() = metadata?.durationMs ?: 0L
}
