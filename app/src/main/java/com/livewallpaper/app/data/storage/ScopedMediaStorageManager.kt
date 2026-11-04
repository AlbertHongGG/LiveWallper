package com.livewallpaper.app.data.storage

import android.content.Context
import android.net.Uri
import com.livewallpaper.app.data.metadata.AndroidThumbnailGenerator
import com.livewallpaper.app.data.metadata.VideoMetadataRetriever
import com.livewallpaper.app.domain.model.ArchivedMediaAsset
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * 沙盒化檔案儲存管理器實作。
 * 採用內容定址儲存 (Content-Addressed Storage, CAS)，以 SHA-256 為唯一鍵管理影片與縮圖庫。
 */
class ScopedMediaStorageManager(
    private val context: Context,
    private val thumbnailGenerator: ThumbnailGenerator = AndroidThumbnailGenerator()
) : MediaStorageManager {

    private val stagingDir: File
        get() = File(context.cacheDir, "staging").apply { if (!exists()) mkdirs() }

    private val archiveVideosDir: File
        get() = File(context.filesDir, "wallpapers/videos").apply { if (!exists()) mkdirs() }

    private val archiveThumbnailsDir: File
        get() = File(context.filesDir, "wallpapers/thumbnails").apply { if (!exists()) mkdirs() }

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

    override suspend fun archiveMedia(sourceFile: File, frameTimeMs: Long): Result<ArchivedMediaAsset> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!sourceFile.exists() || sourceFile.length() == 0L) {
                    throw WallpaperError.StorageError("來源影片檔案不存在或內容為空")
                }

                archiveVideosDir.mkdirs()
                archiveThumbnailsDir.mkdirs()

                val hash = computeSha256(sourceFile)
                val targetVideoFile = File(archiveVideosDir, "video_$hash.mp4")
                val targetThumbnailFile = File(archiveThumbnailsDir, "thumb_$hash.webp")

                // 若目標影片檔案尚未存在，則將來源檔案複製進正式影片庫
                if (!targetVideoFile.exists() || targetVideoFile.length() != sourceFile.length()) {
                    sourceFile.copyTo(targetVideoFile, overwrite = true)
                }

                // 依當前剪輯起點或預設起點生成/刷新 WebP 縮圖
                thumbnailGenerator.generateThumbnail(
                    videoFile = targetVideoFile,
                    destinationFile = targetThumbnailFile,
                    frameTimeMs = frameTimeMs
                ).getOrThrow()

                val metadata = VideoMetadataRetriever.retrieve(targetVideoFile)

                // 自動清理 staging 暫存目錄以釋放快取空間
                clearStaging()

                ArchivedMediaAsset(
                    id = hash,
                    videoFile = targetVideoFile,
                    thumbnailFile = targetThumbnailFile,
                    metadata = metadata
                )
            }.recoverCatching { throwable ->
                if (throwable is WallpaperError) throw throwable
                throw WallpaperError.StorageError("封存媒體資產失敗: ${throwable.localizedMessage}", throwable)
            }
        }

    override suspend fun deleteArchivedMedia(videoFilePath: String, thumbnailFilePath: String): Boolean =
        withContext(Dispatchers.IO) {
            var deleted = false
            if (videoFilePath.isNotBlank()) {
                val vFile = File(videoFilePath)
                if (vFile.exists()) {
                    deleted = vFile.delete() || deleted
                }
            }
            if (thumbnailFilePath.isNotBlank()) {
                val tFile = File(thumbnailFilePath)
                if (tFile.exists()) {
                    tFile.delete()
                }
            }
            deleted
        }

    override suspend fun clearAllArchivedMedia(excludeVideoPath: String?): Unit = withContext(Dispatchers.IO) {
        try {
            val excludeCanonical = excludeVideoPath?.let { File(it).canonicalPath }
            archiveVideosDir.listFiles()?.forEach { file ->
                if (excludeCanonical == null || file.canonicalPath != excludeCanonical) {
                    file.delete()
                }
            }
            val activeHash = excludeCanonical?.substringAfterLast("video_")?.substringBeforeLast(".mp4")
            archiveThumbnailsDir.listFiles()?.forEach { file ->
                if (activeHash == null || !file.name.contains(activeHash)) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
        Unit
    }

    override suspend fun clearStaging(): Unit = withContext(Dispatchers.IO) {
        try {
            stagingDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    override suspend fun getStagingStats(): Pair<Long, Int> = withContext(Dispatchers.IO) {
        try {
            val files = stagingDir.listFiles() ?: emptyArray()
            Pair(files.sumOf { it.length() }, files.size)
        } catch (_: Exception) {
            Pair(0L, 0)
        }
    }

    override suspend fun getArchiveStats(): Pair<Long, Int> = withContext(Dispatchers.IO) {
        try {
            val videoFiles = archiveVideosDir.listFiles() ?: emptyArray()
            val thumbFiles = archiveThumbnailsDir.listFiles() ?: emptyArray()
            val totalSize = videoFiles.sumOf { it.length() } + thumbFiles.sumOf { it.length() }
            Pair(totalSize, videoFiles.size)
        } catch (_: Exception) {
            Pair(0L, 0)
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
