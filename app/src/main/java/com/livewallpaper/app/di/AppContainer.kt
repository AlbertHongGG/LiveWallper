package com.livewallpaper.app.di

import android.content.Context
import com.livewallpaper.app.data.datastore.WallpaperHistoryDataSource
import com.livewallpaper.app.data.datastore.WallpaperPreferencesDataSource
import com.livewallpaper.app.data.extractor.ResilientMediaExtractor
import com.livewallpaper.app.data.extractor.YtDlpExtractorEngineManager
import com.livewallpaper.app.data.extractor.policy.DownloadFormatPolicy
import com.livewallpaper.app.data.extractor.runner.YoutubeDlAndroidRunner
import com.livewallpaper.app.data.extractor.runner.YtDlpRunner
import com.livewallpaper.app.data.extractor.strategy.CompositeRequestCustomizer
import com.livewallpaper.app.data.extractor.strategy.PinterestRequestCustomizer
import com.livewallpaper.app.data.repository.WallpaperHistoryRepositoryImpl
import com.livewallpaper.app.data.repository.WallpaperRepositoryImpl
import com.livewallpaper.app.data.storage.ScopedMediaStorageManager
import com.livewallpaper.app.data.system.AndroidWallpaperPermissionManager
import com.livewallpaper.app.data.system.AndroidWallpaperSystemManager
import com.livewallpaper.app.domain.parser.PinterestUrlParser
import com.livewallpaper.app.domain.parser.SharedUrlResolver
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.livewallpaper.app.domain.repository.MediaExtractor
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperPermissionManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import com.livewallpaper.app.domain.usecase.ApplyWallpaperUseCase
import com.livewallpaper.app.domain.usecase.ClearWallpaperHistoryUseCase
import com.livewallpaper.app.domain.usecase.DeleteHistoryItemUseCase
import com.livewallpaper.app.domain.usecase.ProcessVideoSourceUseCase
import com.livewallpaper.app.domain.usecase.ResetWallpaperUseCase

/**
 * 輕量且型別安全的手動依賴注入容器 (Manual Dependency Injection Container)。
 * 零程式碼生成開銷、零反射、便於快速啟動與單元測試。
 */
class AppContainer(private val context: Context) {

    val mediaStorageManager: MediaStorageManager by lazy {
        ScopedMediaStorageManager(context)
    }

    val extractorEngineManager: ExtractorEngineManager by lazy {
        YtDlpExtractorEngineManager(context)
    }

    val ytDlpRunner: YtDlpRunner by lazy {
        YoutubeDlAndroidRunner()
    }

    val downloadFormatPolicy: DownloadFormatPolicy by lazy {
        DownloadFormatPolicy()
    }

    val requestCustomizer: CompositeRequestCustomizer by lazy {
        CompositeRequestCustomizer(
            customizers = listOf(PinterestRequestCustomizer())
        )
    }

    val mediaExtractor: MediaExtractor by lazy {
        ResilientMediaExtractor(
            runner = ytDlpRunner,
            engineManager = extractorEngineManager,
            requestCustomizer = requestCustomizer,
            formatPolicy = downloadFormatPolicy
        )
    }

    val pinterestUrlParser by lazy {
        PinterestUrlParser()
    }

    val urlParsers by lazy {
        listOf(pinterestUrlParser)
    }

    val sharedUrlResolver by lazy {
        SharedUrlResolver(urlParsers)
    }

    private val preferencesDataSource by lazy {
        WallpaperPreferencesDataSource(context)
    }

    val wallpaperRepository: WallpaperRepository by lazy {
        WallpaperRepositoryImpl(preferencesDataSource)
    }

    private val historyDataSource by lazy {
        WallpaperHistoryDataSource(context)
    }

    val wallpaperHistoryRepository: WallpaperHistoryRepository by lazy {
        WallpaperHistoryRepositoryImpl(historyDataSource, mediaStorageManager)
    }

    val processVideoSourceUseCase: ProcessVideoSourceUseCase by lazy {
        ProcessVideoSourceUseCase(
            storageManager = mediaStorageManager,
            mediaExtractor = mediaExtractor,
            urlResolver = sharedUrlResolver
        )
    }

    val wallpaperPermissionManager: WallpaperPermissionManager by lazy {
        AndroidWallpaperPermissionManager(context)
    }

    val wallpaperSystemManager: WallpaperSystemManager by lazy {
        AndroidWallpaperSystemManager(context)
    }

    val resetWallpaperUseCase: ResetWallpaperUseCase by lazy {
        ResetWallpaperUseCase(
            storageManager = mediaStorageManager,
            wallpaperRepository = wallpaperRepository,
            wallpaperSystemManager = wallpaperSystemManager
        )
    }

    val applyWallpaperUseCase: ApplyWallpaperUseCase by lazy {
        ApplyWallpaperUseCase(
            storageManager = mediaStorageManager,
            wallpaperRepository = wallpaperRepository,
            wallpaperSystemManager = wallpaperSystemManager,
            historyRepository = wallpaperHistoryRepository
        )
    }

    val deleteHistoryItemUseCase: DeleteHistoryItemUseCase by lazy {
        DeleteHistoryItemUseCase(
            historyRepository = wallpaperHistoryRepository,
            wallpaperRepository = wallpaperRepository,
            resetWallpaperUseCase = resetWallpaperUseCase
        )
    }

    val clearWallpaperHistoryUseCase: ClearWallpaperHistoryUseCase by lazy {
        ClearWallpaperHistoryUseCase(
            historyRepository = wallpaperHistoryRepository,
            wallpaperRepository = wallpaperRepository,
            resetWallpaperUseCase = resetWallpaperUseCase
        )
    }
}
