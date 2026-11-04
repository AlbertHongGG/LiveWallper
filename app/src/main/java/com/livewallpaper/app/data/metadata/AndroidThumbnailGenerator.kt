package com.livewallpaper.app.data.metadata

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import com.livewallpaper.app.domain.repository.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 基於 Android MediaMetadataRetriever 的高清輕量 WebP 縮圖生成器。
 */
class AndroidThumbnailGenerator : ThumbnailGenerator {

    override suspend fun generateThumbnail(
        videoFile: File,
        destinationFile: File,
        frameTimeMs: Long,
        maxDimension: Int
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            destinationFile.parentFile?.mkdirs()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoFile.absolutePath)
                val timeUs = if (frameTimeMs > 0L) frameTimeMs * 1000L else 0L
                val rawBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                    ?: throw IllegalStateException("無法擷取影片縮圖影格: ${videoFile.absolutePath}")

                val scaledBitmap = scaleBitmapDown(rawBitmap, maxDimension)
                if (scaledBitmap != rawBitmap) {
                    rawBitmap.recycle()
                }

                @Suppress("DEPRECATION")
                val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }

                FileOutputStream(destinationFile).use { out ->
                    scaledBitmap.compress(compressFormat, 85, out)
                }
                scaledBitmap.recycle()

                destinationFile
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxCurrent = maxOf(width, height)
        if (maxCurrent <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / maxCurrent
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
