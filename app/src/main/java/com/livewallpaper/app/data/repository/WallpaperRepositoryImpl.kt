package com.livewallpaper.app.data.repository

import com.livewallpaper.app.data.datastore.WallpaperPreferencesDataSource
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

class WallpaperRepositoryImpl(
    private val preferencesDataSource: WallpaperPreferencesDataSource
) : WallpaperRepository {

    override val wallpaperConfig: Flow<WallpaperConfig> = preferencesDataSource.configFlow

    override suspend fun getCurrentConfig(): WallpaperConfig {
        return preferencesDataSource.getCurrentConfig()
    }

    override suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
        preferencesDataSource.updateConfig(transform)
    }

    override suspend fun saveConfig(config: WallpaperConfig) {
        preferencesDataSource.saveConfig(config)
    }
}
