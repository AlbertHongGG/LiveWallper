package com.livewallpaper.app.service.engine.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.livewallpaper.app.domain.model.VideoScalePolicy
import com.livewallpaper.app.service.engine.model.WallpaperRenderState

/**
 * 桌布 OpenGL ES 2.0 事件驅動渲染協調器。
 *
 * 核心架構突破：
 * 1. 專屬獨立 GL 執行緒（[HandlerThread]），所有 EGL/GL 呼叫保證在同一執行緒，符合規範且絕不阻塞主執行緒。
 * 2. 100% 事件驅動：透過 [SurfaceTexture.OnFrameAvailableListener] 接收解碼幀，無任何輪詢與多餘重繪。
 * 3. 預覽與桌布幾何雙軌統一：調用 [VideoScalePolicy.calculateNdcScale] 保證精確的純數學中心裁切與滿版。
 * 4. 零延遲遮罩即時響應：動態修改 dimAlpha 時，若已有畫面可立即以最新亮度重繪，免重啟播放器。
 */
class WallpaperGlRenderer {

    private val glThread = HandlerThread("WallpaperGlRenderer").apply { start() }
    private val glHandler = Handler(glThread.looper)

    private val eglEnvironment = WallpaperEglEnvironment()
    private val shaderProgram = VideoFrameShaderProgram()

    private var textureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var outputSurface: Surface? = null

    private var renderState = WallpaperRenderState()
    private val stMatrix = FloatArray(16)
    private var hasReceivedFrame: Boolean = false

    private var onOutputSurfaceReadyListener: ((Surface) -> Unit)? = null
    private var onOutputSurfaceDestroyedListener: (() -> Unit)? = null

    /**
     * 註冊解碼輸出 Surface 就緒監聽器（交由 ExoPlayer.setVideoSurface 使用）。
     */
    fun setOutputSurfaceListener(
        onReady: (Surface) -> Unit,
        onDestroyed: () -> Unit
    ) {
        this.onOutputSurfaceReadyListener = onReady
        this.onOutputSurfaceDestroyedListener = onDestroyed
    }

    /**
     * 系統 WallpaperService Surface 建立事件。
     */
    fun onSurfaceCreated(holder: SurfaceHolder) {
        val surface = holder.surface
        glHandler.post {
            handleSurfaceCreated(surface)
        }
    }

    /**
     * 系統 WallpaperService Surface 尺寸變更事件（包含螢幕旋轉、折疊螢幕展開）。
     */
    fun onSurfaceChanged(width: Int, height: Int) {
        glHandler.post {
            handleSurfaceChanged(width, height)
        }
    }

    /**
     * 系統 WallpaperService Surface 銷毀事件。
     */
    fun onSurfaceDestroyed() {
        // 同步等待或迅速切斷輸出，通知播放器解綁 Surface
        onOutputSurfaceDestroyedListener?.invoke()
        glHandler.post {
            handleSurfaceDestroyed()
        }
    }

    /**
     * 更新渲染狀態（視訊解析度、ScalingMode、dimAlpha 等）。
     */
    fun updateRenderState(newState: WallpaperRenderState) {
        glHandler.post {
            val oldState = renderState
            renderState = newState

            // 若已有紋理畫面且關鍵視覺參數（dimAlpha / scalingMode / zoom / pan / 解析度）改變，立即觸發單次無損重繪
            if (hasReceivedFrame && eglEnvironment.isInitialized && newState.isReadyToRender) {
                if (oldState.dimAlpha != newState.dimAlpha ||
                    oldState.scalingMode != newState.scalingMode ||
                    oldState.zoomScale != newState.zoomScale ||
                    oldState.panOffsetX != newState.panOffsetX ||
                    oldState.panOffsetY != newState.panOffsetY ||
                    oldState.videoWidth != newState.videoWidth ||
                    oldState.videoHeight != newState.videoHeight
                ) {
                    renderCurrentFrame(updateTexImage = false)
                }
            }
        }
    }

    /**
     * 釋放渲染器與 GL 執行緒。
     */
    fun release() {
        onOutputSurfaceDestroyedListener?.invoke()
        onOutputSurfaceReadyListener = null
        onOutputSurfaceDestroyedListener = null

        glHandler.post {
            handleSurfaceDestroyed()
            glThread.quitSafely()
        }
    }

    private fun handleSurfaceCreated(surface: Surface) {
        if (!surface.isValid) return

        if (!eglEnvironment.initialize(surface)) {
            Log.e(TAG, "Failed to initialize EGL environment")
            return
        }

        if (!eglEnvironment.makeCurrent()) {
            Log.e(TAG, "Failed to make EGL environment current")
            eglEnvironment.release()
            return
        }

        if (!shaderProgram.initialize()) {
            Log.e(TAG, "Failed to initialize shader program")
            eglEnvironment.release()
            return
        }

        // 建立外部視訊 OES 紋理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        val st = SurfaceTexture(textureId)
        st.setOnFrameAvailableListener({
            // 由專屬 GL Handler 執行緒事件驅動渲染新畫面
            renderCurrentFrame(updateTexImage = true)
        }, glHandler)

        surfaceTexture = st
        val outSurface = Surface(st)
        outputSurface = outSurface
        hasReceivedFrame = false

        onOutputSurfaceReadyListener?.invoke(outSurface)
    }

    private fun handleSurfaceChanged(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        renderState = renderState.copy(surfaceWidth = width, surfaceHeight = height)

        if (eglEnvironment.isInitialized) {
            eglEnvironment.makeCurrent()
            GLES20.glViewport(0, 0, width, height)
            if (hasReceivedFrame) {
                renderCurrentFrame(updateTexImage = false)
            }
        }
    }

    private fun handleSurfaceDestroyed() {
        hasReceivedFrame = false

        outputSurface?.release()
        outputSurface = null

        surfaceTexture?.release()
        surfaceTexture = null

        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
            textureId = 0
        }

        shaderProgram.release()
        eglEnvironment.release()
    }

    private fun renderCurrentFrame(updateTexImage: Boolean) {
        val st = surfaceTexture ?: return
        if (!eglEnvironment.isInitialized || !renderState.isReadyToRender) return

        if (!eglEnvironment.makeCurrent()) return

        try {
            if (updateTexImage) {
                st.updateTexImage()
                st.getTransformMatrix(stMatrix)
                hasReceivedFrame = true
            }

            // 清屏為純黑底色（確保 FIT 模式留黑邊處或首幀載入時無花屏）
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            // 藉由純幾何數學引擎計算 NDC Quad 縮放與平移變換向量
            val ndcTransform = VideoScalePolicy.calculateNdcTransform(
                containerWidth = renderState.surfaceWidth,
                containerHeight = renderState.surfaceHeight,
                videoWidth = renderState.videoWidth,
                videoHeight = renderState.videoHeight,
                scalingMode = renderState.scalingMode,
                transform = com.livewallpaper.app.domain.model.ViewportTransform(
                    zoomScale = renderState.zoomScale,
                    panOffsetX = renderState.panOffsetX,
                    panOffsetY = renderState.panOffsetY
                )
            )

            // 執行 GPU Quad 繪製與像素級壓暗遮罩
            shaderProgram.drawFrame(
                textureId = textureId,
                stMatrix = stMatrix,
                scaleX = ndcTransform.scaleX,
                scaleY = ndcTransform.scaleY,
                translateX = ndcTransform.translateX,
                translateY = ndcTransform.translateY,
                brightness = renderState.brightnessMultiplier
            )

            eglEnvironment.swapBuffers()
        } catch (e: Exception) {
            Log.w(TAG, "Error rendering frame", e)
        }
    }

    companion object {
        private const val TAG = "WallpaperGlRenderer"
    }
}
