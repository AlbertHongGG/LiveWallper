package com.livewallpaper.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.livewallpaper.app.presentation.theme.AppColors

/**
 * 旗艦級立體觸覺滑桿把手 (Thumb)。
 * 白底 + 2dp 品牌綻藍外框 + 3dp 柔和陰影。
 * 搭配 thumbTrackGapSize = 0.dp，平滑懸浮於連續無縫軌道正上方。
 */
@Composable
fun AppSliderThumb(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 3.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            )
            .background(Color.White, CircleShape)
            .border(2.dp, AppColors.accentIndigo, CircleShape)
    )
}
