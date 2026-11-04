package com.livewallpaper.app.service.engine.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.util.Log
import android.view.Surface

/**
 * 專職封裝 Android 原生 EGL14 繪圖環境與生命週期。
 *
 * 職責：
 * 1. 管理 EGLDisplay、EGLContext (OpenGL ES 2.0) 與 EGLSurface 的建立與銷毀。
 * 2. 封裝上下文切換 [makeCurrent] 與畫面緩衝區交換 [swapBuffers]。
 * 3. 嚴格隔離底層狀態機與資源清理，保證無任何記憶體與圖形控制代碼洩漏。
 */
class WallpaperEglEnvironment {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    val isInitialized: Boolean
        get() = eglDisplay != EGL14.EGL_NO_DISPLAY &&
                eglContext != EGL14.EGL_NO_CONTEXT &&
                eglSurface != EGL14.EGL_NO_SURFACE

    /**
     * 以系統提供之原生 [Surface] 初始化 EGL 環境與 WindowSurface。
     */
    fun initialize(surface: Surface): Boolean {
        if (!surface.isValid) {
            Log.w(TAG, "initialize failed: Surface is not valid")
            return false
        }

        // 1. 取得並初始化預設 EGLDisplay
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "eglGetDisplay failed: ${EGL14.eglGetError()}")
            return false
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            Log.e(TAG, "eglInitialize failed: ${EGL14.eglGetError()}")
            release()
            return false
        }

        // 2. 選擇合適的 RGBA8888 + ES2 組態
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGLExt.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay,
                configAttribs, 0,
                configs, 0,
                configs.size,
                numConfigs, 0
            ) || numConfigs[0] <= 0
        ) {
            Log.e(TAG, "eglChooseConfig failed: ${EGL14.eglGetError()}")
            release()
            return false
        }
        val chosenConfig = configs[0] ?: run {
            Log.e(TAG, "Chosen EGLConfig is null")
            release()
            return false
        }
        eglConfig = chosenConfig

        // 3. 建立 OpenGL ES 2.0 上下文 (EGL_CONTEXT_CLIENT_VERSION = 2)
        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            chosenConfig,
            EGL14.EGL_NO_CONTEXT,
            contextAttribs, 0
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "eglCreateContext failed: ${EGL14.eglGetError()}")
            release()
            return false
        }

        // 4. 建立與原生 Surface 關聯之 EGLWindowSurface
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            chosenConfig,
            surface,
            surfaceAttribs, 0
        )
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "eglCreateWindowSurface failed: ${EGL14.eglGetError()}")
            release()
            return false
        }

        return true
    }

    /**
     * 將 EGL 上下文綁定至當前執行緒。
     */
    fun makeCurrent(): Boolean {
        if (!isInitialized) return false
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG, "eglMakeCurrent failed: ${EGL14.eglGetError()}")
            return false
        }
        return true
    }

    /**
     * 交換畫面緩衝區，將已繪製之畫面呈現在屏幕上。
     */
    fun swapBuffers(): Boolean {
        if (!isInitialized) return false
        val success = EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        if (!success) {
            val error = EGL14.eglGetError()
            if (error != EGL14.EGL_SUCCESS) {
                Log.w(TAG, "eglSwapBuffers warning/error: $error")
            }
        }
        return success
    }

    /**
     * 解除綁定並釋放所有 EGL 相關資源。
     */
    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )

            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }

            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }

            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
        eglConfig = null
    }

    companion object {
        private const val TAG = "WallpaperEglEnv"
    }
}
