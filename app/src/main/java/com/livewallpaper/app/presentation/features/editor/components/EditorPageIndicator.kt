package com.livewallpaper.app.presentation.features.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.livewallpaper.app.presentation.theme.AppColors

/**
 * 桌面 Launcher 風格純圓點指示器。
 * 徹底移除多餘文字標籤，節省垂直空間；支援點擊任意圓點直接跳轉分頁。
 */
@Composable
fun EditorPageIndicator(
    pageCount: Int,
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage

            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 22.dp else 7.dp,
                animationSpec = tween(durationMillis = 250),
                label = "dotWidth"
            )

            val dotColor by animateColorAsState(
                targetValue = if (isSelected) AppColors.accentIndigo else Color(0xFFCBD5E1),
                animationSpec = tween(durationMillis = 250),
                label = "dotColor"
            )

            Box(
                modifier = Modifier
                    .height(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectPage(index)
                            }
                        }
                    )
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(dotWidth)
                        .height(7.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(dotColor)
                )
            }
        }
    }
}
