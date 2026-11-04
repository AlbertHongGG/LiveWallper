package com.livewallpaper.app.domain.repository

import com.livewallpaper.app.domain.model.WallpaperConfig
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    val wallpaperConfig: Flow<WallpaperConfig>

    suspend fun getCurrentConfig(): WallpaperConfig

    suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig)

    suspend fun saveConfig(config: WallpaperConfig)
}
