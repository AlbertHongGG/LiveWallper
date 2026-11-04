package com.livewallpaper.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 應用程式淺色調色盤 (Light Mode First)。
 * 100% 對齊 GeminiFlowApp / LensWise 核心視覺規範。
 */
object AppColors {
    // 淺色底色與卡片
    val backgroundLight = Color(0xFFF5F7FA) // 柔和灰白畫布
    val surfaceLight = Color(0xFFFFFFFF)    // 純白卡片
    val surfaceElevated = Color(0xFFF1F5F9) // 柔和抬升卡片底色

    // 核心主色與強調色
    val primary = Color(0xFF475569)         // 沉穩石板灰 Slate 600
    val secondary = Color(0xFF94A3B8)       // Slate 400
    val accentIndigo = Color(0xFF6366F1)    // 活力綻藍 Indigo (YTFlow 標誌主色)
    val electricBlue = Color(0xFF38BDF8)    // Sky Accent

    // 文字色彩
    val textPrimaryLight = Color(0xFF1A1D2B)   // 深藍墨黑 (高對比標題)
    val textSecondaryLight = Color(0xFF6E768C) // 次要說明灰
    val textMuted = Color(0xFF94A3B8)

    // 語意色彩
    val success = Color(0xFF10B981)         // 翡翠綠 Emerald
    val danger = Color(0xFFEF4444)          // 亮紅 Red Accent
    val warning = Color(0xFFF59E0B)         // 琥珀橘 Amber
    val pinterestRed = Color(0xFFE60023)    // Pinterest 經典紅

    // 邊框與分隔線
    val borderLight = Color(0x0F000000)     // 黑 6% 細微邊框
    val borderMedium = Color(0xFFE2E8F0)    // 結構卡片邊框
    val divider = Color(0x0D000000)         // 黑 5% 縮排分隔線
}
