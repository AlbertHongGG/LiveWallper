package com.livewallpaper.app.presentation.features.editor.components.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.livewallpaper.app.presentation.features.editor.components.ProTimelineSlider
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorAction
import com.livewallpaper.app.presentation.theme.AppColors

@Composable
fun PlaybackSettingsCard(
    config: WallpaperConfig,
    durationMs: Long,
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 片段剪輯開關與時間軸滑桿
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "啟用片段剪輯",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimaryLight
                        )
                        Text(
                            text = if (config.enableTrim) "自訂區間循環播放" else "全片完整循環播放",
                            fontSize = 11.sp,
                            color = AppColors.textSecondaryLight
                        )
                    }

                    AppSwitch(
                        checked = config.enableTrim,
                        onCheckedChange = { onAction(WallpaperEditorAction.ToggleTrim(it)) },
                        enabled = durationMs > 0L
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (config.enableTrim && durationMs > 0L) {
                    ProTimelineSlider(
                        totalDurationMs = durationMs,
                        startMs = config.trimStartMs,
                        endMs = if (config.trimEndMs > 0L) config.trimEndMs else durationMs,
                        onRangeChange = { start, end ->
                            onAction(WallpaperEditorAction.UpdateTrimRange(start, end))
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (durationMs > 0L) "開啟後可設定指定起訖秒數循環" else "正在解析影片時長資訊...",
                            fontSize = 12.sp,
                            color = AppColors.textSecondaryLight
                        )
                    }
                }
            }

            // 2. 播放速度五檔切換按鈕
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "播放速度",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimaryLight
                    )
                    Text(
                        text = "${config.playbackSpeed}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.accentIndigo
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speeds.forEach { speed ->
                        val isSelected = config.playbackSpeed == speed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppColors.accentIndigo else Color(0xFFF1F5F9))
                                .clickable { onAction(WallpaperEditorAction.ChangePlaybackSpeed(speed)) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else AppColors.textSecondaryLight
                            )
                        }
                    }
                }
            }
        }
    }
}
