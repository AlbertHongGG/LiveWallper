package com.livewallpaper.app.data.extractor.policy

import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * 全域下載格式與轉碼策略。
 * 採用 yt-dlp 之排序偏好 (-S) 取代破壞性之硬過濾 (-f)，
 * 確保優先選擇 ExoPlayer 硬體解碼相容之 H.264+AAC，並合併輸出為標準 MP4。
 */
class DownloadFormatPolicy {

    companion object {
        const val FORMAT_SORT_SPEC = "vcodec:h264,res,acodec:aac"
        const val MERGE_OUTPUT_FORMAT = "mp4"
    }

    fun applyFormatOptions(request: YoutubeDLRequest) {
        request.apply {
            addOption("-S", FORMAT_SORT_SPEC)
            addOption("--merge-output-format", MERGE_OUTPUT_FORMAT)
            addOption("--no-playlist")
            addOption("--no-check-certificates")
        }
    }
}
