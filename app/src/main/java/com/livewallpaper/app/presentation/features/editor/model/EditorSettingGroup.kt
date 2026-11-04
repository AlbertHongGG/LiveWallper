package com.livewallpaper.app.presentation.features.editor.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 旋轉木馬 5 大單一職責設定分組（嚴格遵循 SRP，杜絕垂直擠壓與溢出）。
 */
enum class EditorSettingGroup(
    val title: String,
    val icon: ImageVector
) {
    VISUAL(
        title = "畫面比例",
        icon = Icons.Default.AspectRatio
    ),
    TRIM(
        title = "片段剪輯",
        icon = Icons.Default.Crop
    ),
    SPEED(
        title = "播放速度",
        icon = Icons.Default.Speed
    ),
    AUDIO(
        title = "桌布聲音",
        icon = Icons.AutoMirrored.Filled.VolumeUp
    ),
    INTERACTION(
        title = "手勢互動",
        icon = Icons.Default.TouchApp
    )
}
