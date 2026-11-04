package com.livewallpaper.app.presentation.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.theme.AppColors

@Composable
fun EditorTopBar(
    isServiceActive: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左側標題與工作室副標
        Column {
            Text(
                text = "桌布配置",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimaryLight
            )
            Text(
                text = "WALLPAPER STUDIO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textSecondaryLight,
                letterSpacing = 1.2.sp
            )
        }

        // 右側全新「套用」藥丸操作按鈕 (取代舊版放棄變更)
        Row(
            modifier = Modifier
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp), spotColor = AppColors.accentIndigo.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.accentIndigo)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply()
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "套用",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
