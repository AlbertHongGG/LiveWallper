package com.livewallpaper.app.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AppColors.primary,
    onPrimary = AppColors.surfaceLight,
    primaryContainer = AppColors.primary.copy(alpha = 0.1f),
    onPrimaryContainer = AppColors.primary,

    secondary = AppColors.secondary,
    onSecondary = AppColors.surfaceLight,
    secondaryContainer = AppColors.backgroundLight,
    onSecondaryContainer = AppColors.textPrimaryLight,

    tertiary = AppColors.accentIndigo,
    onTertiary = AppColors.surfaceLight,

    background = AppColors.backgroundLight,
    onBackground = AppColors.textPrimaryLight,

    surface = AppColors.surfaceLight,
    onSurface = AppColors.textPrimaryLight,
    surfaceVariant = AppColors.backgroundLight,
    onSurfaceVariant = AppColors.textSecondaryLight,

    outline = AppColors.borderLight,
    outlineVariant = AppColors.secondary.copy(alpha = 0.3f),

    error = AppColors.danger,
    onError = AppColors.surfaceLight
)

@Composable
fun LiveWallpaperTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = AppColors.backgroundLight.toArgb()
            window.navigationBarColor = AppColors.backgroundLight.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
