package com.livewallpaper.app.data.extractor

import android.content.Context
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 基於 youtubedl-android 的底層執行引擎生命週期管理器。
 * 採用 Mutex 保證多協程存取時的線程安全，防止並發初始化與更新衝突。
 */
class YtDlpExtractorEngineManager(
    private val context: Context
) : ExtractorEngineManager {

    private val mutex = Mutex()
    private var isInitialized = false

    override suspend fun ensureInitialized(): Unit = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        mutex.withLock {
            if (!isInitialized) {
                try {
                    YoutubeDL.getInstance().init(context)
                    isInitialized = true
                } catch (_: Exception) {
                    isInitialized = true
                }
            }
        }
    }

    override suspend fun getEngineVersion(): String = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            YoutubeDL.getInstance().version(context) ?: "已就緒"
        } catch (_: Exception) {
            "未初始化"
        }
    }

    override suspend fun updateEngine(): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                ensureInitialized()
                val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
                val updatedVer = YoutubeDL.getInstance().version(context)
                "${status?.name ?: "已完成"} ($updatedVer)"
            }
        }
    }
}
