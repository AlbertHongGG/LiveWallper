package com.livewallpaper.app.presentation.features.editor.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 旋轉木馬設定分組列舉（具備高度可擴展性）。
 * 若未來需擴增「濾鏡風格」或「排程輪播」，只需在此新增項目並實作對應卡片即可自動適配。
 */
enum class EditorSettingGroup(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    VISUAL(
        title = "畫面比例與視覺",
        subtitle = "填充裁切與桌面壓暗",
        icon = Icons.Default.AspectRatio
    ),
    PLAYBACK(
        title = "片段剪輯與速度",
        subtitle = "循環區間與播放倍速",
        icon = Icons.Default.Speed
    ),
    AUDIO_INTERACTION(
        title = "聲音與互動控制",
        subtitle = "桌布音量與雙擊互動",
        icon = Icons.AutoMirrored.Filled.VolumeUp
    )
}
