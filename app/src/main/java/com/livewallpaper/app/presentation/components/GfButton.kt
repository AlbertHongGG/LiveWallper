package com.livewallpaper.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.theme.AppColors

enum class GfButtonVariant {
    Primary,
    Success,
    Danger,
    Secondary,
    Ghost
}

@Composable
fun GfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GfButtonVariant = GfButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    fontSize: TextUnit = 15.sp,
    cornerRadius: Dp = 14.dp
) {
    val (bgColor, contentColor, borderStroke) = when (variant) {
        GfButtonVariant.Primary -> Triple(AppColors.accentIndigo, Color.White, null)
        GfButtonVariant.Success -> Triple(AppColors.success, Color.White, null)
        GfButtonVariant.Danger -> Triple(AppColors.danger, Color.White, null)
        GfButtonVariant.Secondary -> Triple(AppColors.surfaceElevated, AppColors.textPrimaryLight, BorderStroke(1.dp, AppColors.borderLight))
        GfButtonVariant.Ghost -> Triple(Color.Transparent, AppColors.accentIndigo, null)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = contentColor,
            disabledContainerColor = AppColors.surfaceElevated,
            disabledContentColor = AppColors.textMuted
        ),
        border = borderStroke,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize
            )
        }
    }
}
