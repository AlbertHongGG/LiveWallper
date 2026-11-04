package com.livewallpaper.app.presentation.features.editor.components.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.features.editor.components.ProTimelineSlider
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorAction
import com.livewallpaper.app.presentation.theme.AppColors
import java.util.Locale

@Composable
fun TrimSettingsCard(
    config: WallpaperConfig,
    durationMs: Long,
    onAction: (WallpaperEditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveEnd = if (config.trimEndMs > 0L) config.trimEndMs else durationMs
    val isTrimmed = config.trimStartMs > 0L || (effectiveEnd > 0L && effectiveEnd < durationMs)

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
            // 標題列：左側標題，右側顯示總長或一鍵重設按鈕 (無開關模型)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "片段循環區間",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimaryLight
                )

                if (isTrimmed) {
                    Text(
                        text = "重設全片",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.accentIndigo,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onAction(WallpaperEditorAction.UpdateTrimRange(0L, durationMs))
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                } else {
                    val totalSec = (durationMs / 1000).toInt()
                    val formatted = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
                    Text(
                        text = "全片 $formatted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textSecondaryLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 專業時間軸區間微調（常駐可用，無生硬開關與空白佔位）
            ProTimelineSlider(
                totalDurationMs = durationMs,
                startMs = config.trimStartMs,
                endMs = effectiveEnd,
                onRangeChange = { start, end ->
                    onAction(WallpaperEditorAction.UpdateTrimRange(start, end))
                }
            )
        }
    }
}
