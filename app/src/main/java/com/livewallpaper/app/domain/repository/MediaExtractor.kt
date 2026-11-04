package com.livewallpaper.app.domain.repository

import com.livewallpaper.app.domain.model.DownloadProgress
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * 遠端媒體解析與下載介面。
 * 專注於依據正規化網址擷取並下載音視訊至目標檔案。
 */
interface MediaExtractor {
    fun extractAndDownload(url: String, targetFile: File): Flow<DownloadProgress>
}
