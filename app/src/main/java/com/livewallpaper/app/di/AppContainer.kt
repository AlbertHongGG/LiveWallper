package com.livewallpaper.app.di

import android.content.Context
import com.livewallpaper.app.data.datastore.WallpaperPreferencesDataSource
import com.livewallpaper.app.data.extractor.YtDlpMediaExtractor
import com.livewallpaper.app.data.repository.WallpaperRepositoryImpl
import com.livewallpaper.app.data.storage.ScopedMediaStorageManager
import com.livewallpaper.app.domain.parser.PinterestUrlParser
import com.livewallpaper.app.domain.repository.MediaExtractor
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.usecase.ApplyWallpaperUseCase
import com.livewallpaper.app.domain.usecase.ProcessVideoSourceUseCase

/**
 * 輕量且型別安全的手動依賴注入容器 (Manual Dependency Injection Container)。
 * 零程式碼生成開銷、零反射、便於快速啟動與單元測試。
 */
class AppContainer(private val context: Context) {

    val mediaStorageManager: MediaStorageManager by lazy {
        ScopedMediaStorageManager(context)
    }

    val mediaExtractor: MediaExtractor by lazy {
        YtDlpMediaExtractor(context)
    }

    val pinterestUrlParser by lazy {
        PinterestUrlParser()
    }

    val urlParsers by lazy {
        listOf(pinterestUrlParser)
    }

    private val preferencesDataSource by lazy {
        WallpaperPreferencesDataSource(context)
    }

    val wallpaperRepository: WallpaperRepository by lazy {
        WallpaperRepositoryImpl(preferencesDataSource)
    }

    val processVideoSourceUseCase: ProcessVideoSourceUseCase by lazy {
        ProcessVideoSourceUseCase(
            storageManager = mediaStorageManager,
            mediaExtractor = mediaExtractor,
            urlParsers = urlParsers
        )
    }

    val applyWallpaperUseCase: ApplyWallpaperUseCase by lazy {
        ApplyWallpaperUseCase(
            storageManager = mediaStorageManager,
            wallpaperRepository = wallpaperRepository
        )
    }
}
