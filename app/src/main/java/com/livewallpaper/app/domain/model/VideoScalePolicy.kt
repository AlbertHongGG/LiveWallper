package com.livewallpaper.app.domain.model

/**
 * 縮放排版幾何計算結果。
 */
data class ScaleLayoutResult(
    val targetWidth: Int,
    val targetHeight: Int,
    val offsetX: Int,
    val offsetY: Int
)

/**
 * 純數學幾何縮放策略計算器（Pure Geometry Scale Engine）。
 *
 * 核心設計：
 * 1. 100% 純函式運算，完全解耦 Android View 與 ExoPlayer 內部黑盒子。
 * 2. 依據容器尺寸、影片真實解析度與 ScalingMode，產出絕對精準之渲染尺寸與居中偏移。
 * 3. 徹底根除雙重縮放衝突與 Compose AndroidViewHolder 的幾何擠壓問題。
 */
object VideoScalePolicy {

    fun calculate(
        containerWidth: Int,
        containerHeight: Int,
        videoWidth: Int,
        videoHeight: Int,
        scalingMode: ScalingMode
    ): ScaleLayoutResult {
        if (containerWidth <= 0 || containerHeight <= 0) {
            return ScaleLayoutResult(containerWidth, containerHeight, 0, 0)
        }
        if (videoWidth <= 0 || videoHeight <= 0) {
            // 解析度尚未備妥時，預設填滿容器
            return ScaleLayoutResult(containerWidth, containerHeight, 0, 0)
        }

        val containerRatio = containerWidth.toFloat() / containerHeight.toFloat()
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

        val (targetW, targetH) = when (scalingMode) {
            ScalingMode.FIT -> {
                // 完整顯示：等比縮放，確保全片完整納入視野，長邊貼齊，短邊產生對稱黑邊
                if (videoRatio > containerRatio) {
                    // 影片比容器寬（例如橫片放直式容器）：寬度貼齊，高度依比例縮減
                    val h = (containerWidth / videoRatio).toInt()
                    containerWidth to h
                } else {
                    // 影片比容器窄：高度貼齊，寬度依比例縮減
                    val w = (containerHeight * videoRatio).toInt()
                    w to containerHeight
                }
            }
            ScalingMode.FILL -> {
                // 滿版裁切：等比縮放，短邊完全填滿，長邊等比放大超出由父層裁切，保證絕對零拉伸！
                if (videoRatio > containerRatio) {
                    // 影片比容器寬（例如 9:16 影片置於 9:20 容器）：高度貼齊，寬度等比放大超出
                    val w = (containerHeight * videoRatio).toInt()
                    w to containerHeight
                } else {
                    // 影片比容器窄：寬度貼齊，高度等比放大超出
                    val h = (containerWidth / videoRatio).toInt()
                    containerWidth to h
                }
            }
        }

        val offsetX = (containerWidth - targetW) / 2
        val offsetY = (containerHeight - targetH) / 2

        return ScaleLayoutResult(
            targetWidth = targetW,
            targetHeight = targetH,
            offsetX = offsetX,
            offsetY = offsetY
        )
    }
}
