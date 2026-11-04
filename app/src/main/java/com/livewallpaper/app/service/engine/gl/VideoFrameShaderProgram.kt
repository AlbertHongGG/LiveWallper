package com.livewallpaper.app.service.engine.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 視訊幀 OpenGL ES 2.0 著色器管理物件。
 *
 * 職責：
 * 1. 編譯與連結 OES 外部紋理採樣之 Vertex Shader 與 Fragment Shader。
 * 2. 封裝 Quad 幾何頂點緩衝區與 UV 紋理座標緩衝區。
 * 3. 執行像素繪製，透過 [uScale] 實現純數學幾何縮放，透過 [uBrightness] 實現像素級無損壓暗遮罩。
 */
class VideoFrameShaderProgram {

    private var programHandle: Int = 0
    private var uStMatrixHandle: Int = -1
    private var uScaleHandle: Int = -1
    private var uTranslateHandle: Int = -1
    private var uBrightnessHandle: Int = -1
    private var uTextureHandle: Int = -1
    private var aPositionHandle: Int = -1
    private var aTexCoordHandle: Int = -1

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_VERTICES)
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUAD_TEX_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_TEX_COORDS)
            position(0)
        }

    val isInitialized: Boolean
        get() = programHandle != 0

    /**
     * 初始化並連結著色器程式。
     */
    fun initialize(): Boolean {
        if (isInitialized) return true

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        if (vertexShader == 0) return false

        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return false
        }

        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "glCreateProgram failed")
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return false
        }

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "glLinkProgram failed: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return false
        }

        // 標記著色器在連結完成後待釋放
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        programHandle = program
        uStMatrixHandle = GLES20.glGetUniformLocation(program, "uStMatrix")
        uScaleHandle = GLES20.glGetUniformLocation(program, "uScale")
        uTranslateHandle = GLES20.glGetUniformLocation(program, "uTranslate")
        uBrightnessHandle = GLES20.glGetUniformLocation(program, "uBrightness")
        uTextureHandle = GLES20.glGetUniformLocation(program, "sTexture")

        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")

        return true
    }

    /**
     * 執行單影格繪製。
     *
     * @param textureId OES 外部紋理識別碼
     * @param stMatrix 由 SurfaceTexture.getTransformMatrix 產生之 4x4 紋理變換矩陣 (16 floats)
     * @param scaleX NDC X 軸縮放係數 (由 VideoScalePolicy 計算)
     * @param scaleY NDC Y 軸縮放係數 (由 VideoScalePolicy 計算)
     * @param translateX NDC X 軸平移向量 (由 VideoScalePolicy 計算)
     * @param translateY NDC Y 軸平移向量 (由 VideoScalePolicy 計算)
     * @param brightness 像素亮度乘數 (1.0 - dimAlpha)
     */
    fun drawFrame(
        textureId: Int,
        stMatrix: FloatArray,
        scaleX: Float,
        scaleY: Float,
        translateX: Float = 0f,
        translateY: Float = 0f,
        brightness: Float
    ) {
        if (!isInitialized) return

        GLES20.glUseProgram(programHandle)

        // 綁定外部視訊紋理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(uTextureHandle, 0)

        // 設定動態轉換矩陣與縮放/平移/壓暗參數
        GLES20.glUniformMatrix4fv(uStMatrixHandle, 1, false, stMatrix, 0)
        GLES20.glUniform2f(uScaleHandle, scaleX, scaleY)
        GLES20.glUniform2f(uTranslateHandle, translateX, translateY)
        GLES20.glUniform1f(uBrightnessHandle, brightness.coerceIn(0f, 1f))

        // 傳遞幾何頂點
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(
            aPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // 傳遞 UV 紋理座標
        texCoordBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)
        GLES20.glVertexAttribPointer(
            aTexCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texCoordBuffer
        )

        // 繪製四邊形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    /**
     * 釋放著色器程式資源。
     */
    fun release() {
        if (programHandle != 0) {
            GLES20.glDeleteProgram(programHandle)
            programHandle = 0
        }
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "glCreateShader failed for type $type")
            return 0
        }
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(TAG, "glCompileShader failed ($type): ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val TAG = "VideoFrameShader"

        // 覆蓋滿屏 NDC 空間 [-1.0, 1.0] 之三角形條帶 (Triangle Strip) 頂點
        private val QUAD_VERTICES = floatArrayOf(
            -1.0f, -1.0f, 0.0f, // 0: 左下
             1.0f, -1.0f, 0.0f, // 1: 右下
            -1.0f,  1.0f, 0.0f, // 2: 左上
             1.0f,  1.0f, 0.0f  // 3: 右上
        )

        // 標準 UV 紋理貼圖座標 (由 SurfaceTexture 變換矩陣校正方向與旋轉)
        private val QUAD_TEX_COORDS = floatArrayOf(
            0.0f, 0.0f, // 0: 左下
            1.0f, 0.0f, // 1: 右下
            0.0f, 1.0f, // 2: 左上
            1.0f, 1.0f  // 3: 右上
        )

        private const val VERTEX_SHADER_CODE = """
            uniform mat4 uStMatrix;
            uniform vec2 uScale;
            uniform vec2 uTranslate;
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            varying vec2 vTexCoord;

            void main() {
                gl_Position = vec4(aPosition.xy * uScale + uTranslate, aPosition.zw);
                vTexCoord = (uStMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER_CODE = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            uniform float uBrightness;

            void main() {
                vec4 color = texture2D(sTexture, vTexCoord);
                gl_FragColor = vec4(color.rgb * uBrightness, color.a);
            }
        """
    }
}
