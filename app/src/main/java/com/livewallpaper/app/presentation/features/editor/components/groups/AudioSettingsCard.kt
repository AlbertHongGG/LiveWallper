package com.livewallpaper.app.presentation.features.editor.components.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.components.AppSliderThumb
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorAction
import com.livewallpaper.app.presentation.theme.AppColors

@Composable
fun AudioSettingsCard(
    config: WallpaperConfig,
    onAction: (WallpaperEditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isActuallyMuted = config.isMuted || config.volume == 0f
    val displayVolume = if (isActuallyMuted) 0f else config.volume

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.surfaceLight,
        border = BorderStroke(1.dp, AppColors.borderMedium),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 標題與當前音量狀態 (預設靜音)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "桌布音量",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimaryLight
                )

                Text(
                    text = if (isActuallyMuted) "已靜音" else "${(config.volume * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActuallyMuted) AppColors.textSecondaryLight else AppColors.accentIndigo
                )
            }

            // 一體化調音台：左側點擊靜音/復原，右側無縫連續滑桿
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 喇叭圖示按鈕 (一鍵靜音 / 復原)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isActuallyMuted) {
                                onAction(WallpaperEditorAction.ToggleMute(false))
                                if (config.volume == 0f) {
                                    onAction(WallpaperEditorAction.ChangeVolume(0.5f))
                                }
                            } else {
                                onAction(WallpaperEditorAction.ToggleMute(true))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActuallyMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "切換靜音",
                        tint = if (isActuallyMuted) AppColors.textSecondaryLight else AppColors.accentIndigo,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 連續無縫音量滑桿 (滑動大於 0 自動開音，滑至 0 自動標註靜音)
                Slider(
                    value = displayVolume,
                    onValueChange = { newVol ->
                        if (newVol > 0f) {
                            if (config.isMuted) {
                                onAction(WallpaperEditorAction.ToggleMute(false))
                            }
                            onAction(WallpaperEditorAction.ChangeVolume(newVol))
                        } else {
                            onAction(WallpaperEditorAction.ToggleMute(true))
                            onAction(WallpaperEditorAction.ChangeVolume(0f))
                        }
                    },
                    valueRange = 0f..1f,
                    thumb = { AppSliderThumb() },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            thumbTrackGapSize = 0.dp,
                            drawStopIndicator = null,
                            colors = SliderDefaults.colors(
                                activeTrackColor = AppColors.accentIndigo,
                                inactiveTrackColor = Color(0xFFE2E8F0)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 簡潔操作引導
            Text(
                text = if (isActuallyMuted) "預設靜音，拖曳滑桿或輕觸喇叭即可開啟" else "桌布背景音效播放中",
                fontSize = 12.sp,
                color = AppColors.textSecondaryLight
            )
        }
    }
}
