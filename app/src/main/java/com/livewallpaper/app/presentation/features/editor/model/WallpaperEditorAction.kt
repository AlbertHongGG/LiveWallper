package com.livewallpaper.app.presentation.features.editor.model

import com.livewallpaper.app.domain.model.ScalingMode

/**
 * 編輯器所有使用者意圖動作 (UDF Actions)。
 */
sealed interface WallpaperEditorAction {
    data class ChangeScalingMode(val mode: ScalingMode) : WallpaperEditorAction
    data class ChangeDimAlpha(val alpha: Float) : WallpaperEditorAction
    data class ChangePlaybackSpeed(val speed: Float) : WallpaperEditorAction
    data class ToggleTrim(val enabled: Boolean) : WallpaperEditorAction
    data class UpdateTrimRange(val startMs: Long, val endMs: Long) : WallpaperEditorAction
    data class ToggleMute(val isMuted: Boolean) : WallpaperEditorAction
    data class ChangeVolume(val volume: Float) : WallpaperEditorAction
    data class ToggleDoubleTapToPause(val enabled: Boolean) : WallpaperEditorAction
}
