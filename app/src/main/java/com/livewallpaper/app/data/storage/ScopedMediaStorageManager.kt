package com.livewallpaper.app.data.storage

import android.content.Context
import android.net.Uri
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.repository.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 沙盒化檔案儲存管理器實作。
 * 統一管理暫存與正式動態桌布檔案，確保 I/O 安全與磁碟空間自動清理。
 */
class ScopedMediaStorageManager(
    private val context: Context
) : MediaStorageManager {

    private val stagingDir: File
        get() = File(context.cacheDir, "staging").apply { if (!exists()) mkdirs() }

    private val activeWallpapersDir: File
        get() = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }

    override suspend fun prepareStagingFile(): File = withContext(Dispatchers.IO) {
        try {
            stagingDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
        File(stagingDir, "staging_preview.mp4")
    }

    override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw WallpaperError.StorageError("無法開啟選定的影片檔案來源 ($uriString)")

                destinationFile.parentFile?.mkdirs()
                FileOutputStream(destinationFile).use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                destinationFile
            }.recoverCatching { throwable ->
                if (throwable is WallpaperError) throw throwable
                throw WallpaperError.StorageError("複製影片檔案失敗: ${throwable.localizedMessage}", throwable)
            }
        }

    override suspend fun promoteStagingToActive(stagingFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!stagingFile.exists() || stagingFile.length() == 0L) {
                    throw WallpaperError.StorageError("預覽暫存檔案不存在或內容為空")
                }

                // 產生帶時間戳的正式桌布檔名，確保路徑變更可作為 DataStore 變更標識
                val timestamp = System.currentTimeMillis()
                val targetFile = File(activeWallpapersDir, "active_wallpaper_$timestamp.mp4")

                stagingFile.copyTo(targetFile, overwrite = true)

                // 自動清理舊的桌布檔，只保留當前這份正式檔案
                cleanupOldWallpapers(targetFile)

                targetFile
            }.recoverCatching { throwable ->
                if (throwable is WallpaperError) throw throwable
                throw WallpaperError.StorageError("套用桌布儲存失敗: ${throwable.localizedMessage}", throwable)
            }
        }

    override suspend fun cleanupOldWallpapers(keepActiveFile: File) = withContext(Dispatchers.IO) {
        try {
            val allFiles = activeWallpapersDir.listFiles() ?: return@withContext
            for (file in allFiles) {
                if (file.absolutePath != keepActiveFile.absolutePath) {
                    file.delete()
                }
            }
            // 清理暫存檔
            stagingDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
            // 靜默處理清理異常，不阻斷核心套用流程
        }
    }

    override suspend fun clearActiveWallpapers(): Unit = withContext(Dispatchers.IO) {
        try {
            activeWallpapersDir.listFiles()?.forEach { it.delete() }
            stagingDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
            // 靜默處理清理異常
        }
        Unit
    }
}
