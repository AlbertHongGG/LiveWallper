package com.livewallpaper.app.presentation.navigation.model

import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperSourceType

/**
 * 應用程式全域導航路由模型。
 */
sealed interface AppRoute {
    val key: String

    /** 主畫面 (HomeScreen) */
    data object Main : AppRoute {
        override val key: String = "route_main"
    }

    /** 桌布配置工作台 (WallpaperEditorScreen) */
    data class Editor(
        val videoPath: String,
        val sourceType: WallpaperSourceType = WallpaperSourceType.LOCAL_GALLERY,
        val sourceUrl: String? = null,
        val presetConfig: WallpaperConfig? = null
    ) : AppRoute {
        override val key: String = "route_editor_${videoPath.hashCode()}"
    }

    /** 獨立系統設定頁 (SettingsPage) */
    data object Settings : AppRoute {
        override val key: String = "route_settings"
    }

    /** 歷史紀錄畫廊 (HistoryGalleryScreen) */
    data object History : AppRoute {
        override val key: String = "route_history"
    }
}
