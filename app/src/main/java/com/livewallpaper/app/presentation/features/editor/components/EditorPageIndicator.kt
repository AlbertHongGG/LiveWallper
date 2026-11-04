package com.livewallpaper.app.presentation.features.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.livewallpaper.app.presentation.features.editor.model.EditorSettingGroup
import com.livewallpaper.app.presentation.theme.AppColors

@Composable
fun EditorPageIndicator(
    pageCount: Int,
    currentPage: Int,
    currentGroup: EditorSettingGroup,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 當前分組名稱標籤提示
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentGroup.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimaryLight
            )
            Text(
                text = " · ${currentPage + 1}/$pageCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textSecondaryLight
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 桌面風格點點點指示器 (支援點擊快速跳轉)
        Row(
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
                        .height(28.dp) // 充足的手勢觸控熱區
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
                        .padding(vertical = 10.dp),
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
}
