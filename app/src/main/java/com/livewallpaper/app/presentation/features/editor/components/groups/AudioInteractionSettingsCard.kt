package com.livewallpaper.app.presentation.features.editor.components.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.components.AppSwitch
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorAction
import com.livewallpaper.app.presentation.theme.AppColors

@Composable
fun AudioInteractionSettingsCard(
    config: WallpaperConfig,
    onAction: (WallpaperEditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 桌布背景音效與音量
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "開啟桌布聲音",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimaryLight
                        )
                        Text(
                            text = if (!config.isMuted) "已啟用桌布背景聲音" else "預設靜音以省電不打擾",
                            fontSize = 11.sp,
                            color = AppColors.textSecondaryLight
                        )
                    }

                    AppSwitch(
                        checked = !config.isMuted,
                        onCheckedChange = { onAction(WallpaperEditorAction.ToggleMute(!it)) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (!config.isMuted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "音量大小",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.textSecondaryLight
                        )
                        Text(
                            text = "${(config.volume * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.accentIndigo
                        )
                    }
                    Slider(
                        value = config.volume,
                        onValueChange = { onAction(WallpaperEditorAction.ChangeVolume(it)) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = AppColors.accentIndigo,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "桌布處於靜音模式，不會播放音效",
                            fontSize = 12.sp,
                            color = AppColors.textSecondaryLight
                        )
                    }
                }
            }

            // 2. 雙擊桌面暫停/恢復播放
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "雙擊桌面暫停播放",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimaryLight
                    )
                    Text(
                        text = "在主螢幕雙擊任意處切換播放與暫停",
                        fontSize = 11.sp,
                        color = AppColors.textSecondaryLight
                    )
                }

                AppSwitch(
                    checked = config.doubleTapToPause,
                    onCheckedChange = { onAction(WallpaperEditorAction.ToggleDoubleTapToPause(it)) }
                )
            }
        }
    }
}
