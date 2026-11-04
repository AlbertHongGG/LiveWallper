package com.livewallpaper.app.domain.repository

import com.livewallpaper.app.domain.model.DownloadProgress
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MediaExtractor {
    fun extractAndDownload(url: String, targetFile: File): Flow<DownloadProgress>

    suspend fun getEngineVersion(): String

    suspend fun updateExtractorEngine(): Result<String>

    suspend fun getCacheStats(): Pair<Long, Int>

    suspend fun clearCache()
}
