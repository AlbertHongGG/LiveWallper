package com.livewallpaper.app.presentation.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.theme.AppColors
import java.util.Locale

@Composable
fun ProTimelineSlider(
    totalDurationMs: Long,
    startMs: Long,
    endMs: Long,
    onRangeChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxMs = if (totalDurationMs > 0L) totalDurationMs.toFloat() else 1000f
    val currentStart = startMs.toFloat().coerceIn(0f, maxMs)
    val currentEnd = (if (endMs > 0L) endMs.toFloat() else maxMs).coerceIn(currentStart, maxMs)

    Column(modifier = modifier.fillMaxWidth()) {
        // 開始與結束時間標籤
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeBadge(label = "開始", timeMs = currentStart.toLong())
            TimeBadge(label = "結束", timeMs = currentEnd.toLong())
        }

        Spacer(modifier = Modifier.height(6.dp))

        RangeSlider(
            value = currentStart..currentEnd,
            onValueChange = { range ->
                onRangeChange(range.start.toLong(), range.endInclusive.toLong())
            },
            valueRange = 0f..maxMs,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = AppColors.accentIndigo,
                inactiveTrackColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TimeBadge(label: String, timeMs: Long) {
    val totalSeconds = (timeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textSecondaryLight
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AppColors.textPrimaryLight
            )
        }
    }
}
