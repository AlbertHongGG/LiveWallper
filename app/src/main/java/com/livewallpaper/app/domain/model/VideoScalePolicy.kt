package com.livewallpaper.app.domain.model

/**
 * 視角幾何仿射變換值物件 (Immutable Value Object)。
 *
 * @param zoomScale 視訊放大倍率 (1.0f ~ 3.0f)
 * @param panOffsetX 歸一化水平平移比例 (-1.0f: 最左邊界, 0.0f: 置中, +1.0f: 最右邊界)
 * @param panOffsetY 歸一化垂直平移比例 (-1.0f: 最上邊界, 0.0f: 置中, +1.0f: 最下邊界)
 */
data class ViewportTransform(
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f
) {
    companion object {
        val IDENTITY = ViewportTransform(zoomScale = 1.0f, panOffsetX = 0f, panOffsetY = 0f)
    }
}

/**
 * 專為 Compose UI 雙階段渲染設計之變換結果。
 *
 * 核心機制：
 * 1. [baseWidth] 與 [baseHeight]：僅供 .layout 階段測量 TextureView 1.0x 基礎尺寸，手勢期間永不變更。
 * 2. [scale]、[translationX] 與 [translationY]：供 Modifier.graphicsLayer 在 GPU RenderNode 執行亞像素平滑變換，0 次 View Relayout。
 */
data class SurfaceTransformResult(
    val baseWidth: Int,
    val baseHeight: Int,
    val baseOffsetX: Int,
    val baseOffsetY: Int,
    val scale: Float,
    val translationX: Float,
    val translationY: Float
)

/**
 * 專為 OpenGL ES 2.0 頂點著色器設計之 NDC 空間變換結果。
 */
data class NdcTransformResult(
    val scaleX: Float,
    val scaleY: Float,
    val translateX: Float,
    val translateY: Float
)

/**
 * 純數學幾何縮放與手勢變換計算引擎 (Pure Geometry Scale Engine)。
 *
 * 核心職責：
 * 1. 100% 純函式運算，完全解耦 Android View 樹與硬體解碼器。
 * 2. 精確計算基礎佈局尺寸與連續 GPU 矩陣位移，杜絕 TextureView 物理重排導致的掉幀。
 * 3. 封裝連續手勢增量演算法與滿版邊界防禦（Clamping），防止露黑底並根除閉包狀態跳動。
 * 4. 確保 Compose 預覽與系統桌面 OpenGL 頂點著色器 100% 數學同源。
 */
object VideoScalePolicy {

    /**
     * 計算在基準狀態 (zoom=1.0) 下符合容器比例之視訊未裁切物理尺寸。
     */
    fun computeBaseDimensions(
        containerWidth: Float,
        containerHeight: Float,
        videoWidth: Int,
        videoHeight: Int,
        scalingMode: ScalingMode
    ): Pair<Float, Float> {
        if (containerWidth <= 0f || containerHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) {
            return containerWidth to containerHeight
        }

        val containerRatio = containerWidth / containerHeight
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

        return when (scalingMode) {
            ScalingMode.FIT -> {
                if (videoRatio > containerRatio) {
                    val h = containerWidth / videoRatio
                    containerWidth to h
                } else {
                    val w = containerHeight * videoRatio
                    w to containerHeight
                }
            }
            ScalingMode.FILL -> {
                if (videoRatio > containerRatio) {
                    val w = containerHeight * videoRatio
                    w to containerHeight
                } else {
                    val h = containerWidth / videoRatio
                    containerWidth to h
                }
            }
        }
    }

    /**
     * 計算供 Compose 雙階段渲染 (Layout + graphicsLayer) 之幾何參數。
     */
    fun calculateSurfaceTransform(
        containerWidth: Int,
        containerHeight: Int,
        videoWidth: Int,
        videoHeight: Int,
        scalingMode: ScalingMode,
        transform: ViewportTransform = ViewportTransform.IDENTITY
    ): SurfaceTransformResult {
        if (containerWidth <= 0 || containerHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            return SurfaceTransformResult(
                baseWidth = containerWidth,
                baseHeight = containerHeight,
                baseOffsetX = 0,
                baseOffsetY = 0,
                scale = 1.0f,
                translationX = 0f,
                translationY = 0f
            )
        }

        val (baseW, baseH) = computeBaseDimensions(
            containerWidth.toFloat(),
            containerHeight.toFloat(),
            videoWidth,
            videoHeight,
            scalingMode
        )

        val baseWidthInt = baseW.toInt().coerceAtLeast(1)
        val baseHeightInt = baseH.toInt().coerceAtLeast(1)
        val baseOffsetX = (containerWidth - baseWidthInt) / 2
        val baseOffsetY = (containerHeight - baseHeightInt) / 2

        val clampedZoom = transform.zoomScale.coerceIn(1.0f, 3.0f)
        val renderedW = baseWidthInt * clampedZoom
        val renderedH = baseHeightInt * clampedZoom

        val maxPanX = if (renderedW > containerWidth) (renderedW - containerWidth) / 2f else 0f
        val maxPanY = if (renderedH > containerHeight) (renderedH - containerHeight) / 2f else 0f

        val translationX = transform.panOffsetX.coerceIn(-1.0f, 1.0f) * maxPanX
        val translationY = transform.panOffsetY.coerceIn(-1.0f, 1.0f) * maxPanY

        return SurfaceTransformResult(
            baseWidth = baseWidthInt,
            baseHeight = baseHeightInt,
            baseOffsetX = baseOffsetX,
            baseOffsetY = baseOffsetY,
            scale = clampedZoom,
            translationX = translationX,
            translationY = translationY
        )
    }

    /**
     * 計算在 OpenGL NDC 空間 [-1.0, 1.0] 下 Quad 頂點之縮放與平移變換向量。
     */
    fun calculateNdcTransform(
        containerWidth: Int,
        containerHeight: Int,
        videoWidth: Int,
        videoHeight: Int,
        scalingMode: ScalingMode,
        transform: ViewportTransform = ViewportTransform.IDENTITY
    ): NdcTransformResult {
        if (containerWidth <= 0 || containerHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            return NdcTransformResult(1.0f, 1.0f, 0f, 0f)
        }

        val surfaceTransform = calculateSurfaceTransform(
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            scalingMode = scalingMode,
            transform = transform
        )

        val scaleX = (surfaceTransform.baseWidth * surfaceTransform.scale) / containerWidth.toFloat()
        val scaleY = (surfaceTransform.baseHeight * surfaceTransform.scale) / containerHeight.toFloat()

        val translateX = (2.0f * surfaceTransform.translationX) / containerWidth.toFloat()
        // 螢幕 Y 軸向下，OpenGL NDC Y 軸向上
        val translateY = -(2.0f * surfaceTransform.translationY) / containerHeight.toFloat()

        return NdcTransformResult(
            scaleX = scaleX,
            scaleY = scaleY,
            translateX = translateX,
            translateY = translateY
        )
    }

    /**
     * 純函式手勢狀態演進器：接收手勢增量，以物理幾何錨點計算出下一幀的 [ViewportTransform]。
     *
     * @param current 當前變換狀態
     * @param containerWidth 容器像素寬
     * @param containerHeight 容器像素高
     * @param videoWidth 視訊原始像素寬
     * @param videoHeight 視訊原始像素高
     * @param scalingMode 縮放模式
     * @param zoomChange 單幀捏合縮放增量 (例如 1.02f)
     * @param panChangeX 單幀手指水平拖曳像素增量 (例如 +5.0f)
     * @param panChangeY 單幀手指垂直拖曳像素增量 (例如 -3.0f)
     */
    fun applyGestureDelta(
        current: ViewportTransform,
        containerWidth: Float,
        containerHeight: Float,
        videoWidth: Int,
        videoHeight: Int,
        scalingMode: ScalingMode,
        zoomChange: Float,
        panChangeX: Float,
        panChangeY: Float
    ): ViewportTransform {
        if (containerWidth <= 0f || containerHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) {
            return current
        }

        val (baseW, baseH) = computeBaseDimensions(
            containerWidth,
            containerHeight,
            videoWidth,
            videoHeight,
            scalingMode
        )

        val oldZoom = current.zoomScale.coerceIn(1.0f, 3.0f)
        val oldRenderedW = baseW * oldZoom
        val oldRenderedH = baseH * oldZoom

        val oldMaxPanX = if (oldRenderedW > containerWidth) (oldRenderedW - containerWidth) / 2f else 0f
        val oldMaxPanY = if (oldRenderedH > containerHeight) (oldRenderedH - containerHeight) / 2f else 0f

        val oldTranslationX = current.panOffsetX.coerceIn(-1.0f, 1.0f) * oldMaxPanX
        val oldTranslationY = current.panOffsetY.coerceIn(-1.0f, 1.0f) * oldMaxPanY

        val newZoom = (oldZoom * zoomChange).coerceIn(1.0f, 3.0f)
        val zoomRatio = if (oldZoom > 0f) newZoom / oldZoom else 1.0f

        val newTranslationX = oldTranslationX * zoomRatio + panChangeX
        val newTranslationY = oldTranslationY * zoomRatio + panChangeY

        val newRenderedW = baseW * newZoom
        val newRenderedH = baseH * newZoom

        val newMaxPanX = if (newRenderedW > containerWidth) (newRenderedW - containerWidth) / 2f else 0f
        val newMaxPanY = if (newRenderedH > containerHeight) (newRenderedH - containerHeight) / 2f else 0f

        val newPanOffsetX = if (newMaxPanX > 0f) {
            (newTranslationX / newMaxPanX).coerceIn(-1.0f, 1.0f)
        } else {
            0f
        }

        val newPanOffsetY = if (newMaxPanY > 0f) {
            (newTranslationY / newMaxPanY).coerceIn(-1.0f, 1.0f)
        } else {
            0f
        }

        return ViewportTransform(
            zoomScale = newZoom,
            panOffsetX = newPanOffsetX,
            panOffsetY = newPanOffsetY
        )
    }
}
