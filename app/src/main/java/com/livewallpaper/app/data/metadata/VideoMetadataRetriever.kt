package com.livewallpaper.app.data.metadata

import android.media.MediaMetadataRetriever
import com.livewallpaper.app.domain.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoMetadataRetriever {

    suspend fun retrieve(file: File): VideoMetadata = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext VideoMetadata(file)
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: "0"
            val rotation = rotationStr.toIntOrNull() ?: 0

            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

            // 如果有 90 或 270 度旋轉，寬高對調
            val (width, height) = if (rotation == 90 || rotation == 270) {
                Pair(rawHeight, rawWidth)
            } else {
                Pair(rawWidth, rawHeight)
            }

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            VideoMetadata(
                file = file,
                width = width,
                height = height,
                durationMs = duration,
                fileSizeBytes = file.length()
            )
        } catch (_: Exception) {
            VideoMetadata(file = file, fileSizeBytes = file.length())
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
