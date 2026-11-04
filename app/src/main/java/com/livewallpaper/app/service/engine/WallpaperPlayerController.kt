package com.livewallpaper.app.service.engine

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.service.engine.gl.WallpaperGlRenderer
import com.livewallpaper.app.service.engine.model.WallpaperRenderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * 旗艦級動態桌布播放控制器。
 *
 * 核心重構亮點：
 * 1. 徹底解耦解碼與渲染：播放器只將視訊幀解碼至 [WallpaperGlRenderer] 產出之 SurfaceTexture 輸出 Surface。
 * 2. 像素級遮罩與幾何中心裁切交由 OpenGL 管線無損執行，解決 Media3 Effects 與原生縮放衝突。
 * 3. 完美支援即時動態遮罩 (dimAlpha) 調節：零延遲、免重啟播放器。
 * 4. 智慧深層省電機制：離開桌面後延遲釋放硬體解碼器，進入桌面秒級喚醒無卡頓。
 */
@OptIn(UnstableApi::class)
class WallpaperPlayerController(
    private val context: Context,
    private val engineScope: CoroutineScope
) {
    private val glRenderer = WallpaperGlRenderer()

    private var exoPlayer: ExoPlayer? = null
    private var activeOutputSurface: Surface? = null
    private var currentConfig: WallpaperConfig? = null

    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    private var savedPosition: Long = 0L
    private var isSurfaceAvailable: Boolean = false
    private var isVisible: Boolean = false
    private var isManuallyPaused: Boolean = false

    private var deepPowerSavingJob: Job? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val config = currentConfig ?: return false
            if (config.doubleTapToPause) {
                toggleManualPlayPause()
                return true
            }
            return false
        }
    })

    init {
        glRenderer.setOutputSurfaceListener(
            onReady = { surface ->
                activeOutputSurface = surface
                engineScope.launch(Dispatchers.Main.immediate) {
                    exoPlayer?.setVideoSurface(surface)
                    if (isVisible && isSurfaceAvailable && !isManuallyPaused) {
                        resumeOrInitPlayer()
                    }
                }
            },
            onDestroyed = {
                activeOutputSurface = null
                engineScope.launch(Dispatchers.Main.immediate) {
                    exoPlayer?.clearVideoSurface()
                }
            }
        )
    }

    fun onTouchEvent(event: MotionEvent) {
        gestureDetector.onTouchEvent(event)
    }

    private fun toggleManualPlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            isManuallyPaused = true
        } else {
            player.play()
            isManuallyPaused = false
        }
    }

    fun onSurfaceCreated(holder: SurfaceHolder) {
        isSurfaceAvailable = true
        glRenderer.onSurfaceCreated(holder)
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        glRenderer.onSurfaceChanged(width, height)
        syncRenderState()
    }

    fun onSurfaceDestroyed() {
        isSurfaceAvailable = false
        deepPowerSavingJob?.cancel()
        glRenderer.onSurfaceDestroyed()
        releasePlayerInternal(savePosition = true)
    }

    fun onVisibilityChanged(visible: Boolean) {
        isVisible = visible
        if (visible) {
            deepPowerSavingJob?.cancel()
            if (isSurfaceAvailable && !isManuallyPaused) {
                resumeOrInitPlayer()
            }
        } else {
            exoPlayer?.pause()

            deepPowerSavingJob?.cancel()
            val delaySeconds = currentConfig?.powerSaveDelaySeconds ?: 3
            deepPowerSavingJob = engineScope.launch {
                delay(delaySeconds * 1000L)
                if (!isVisible) {
                    releasePlayerInternal(savePosition = true)
                }
            }
        }
    }

    fun onConfigChanged(newConfig: WallpaperConfig) {
        val oldConfig = currentConfig
        currentConfig = newConfig

        if (!newConfig.hasVideo) {
            releasePlayerInternal(savePosition = false)
            return
        }

        // 同步縮放與遮罩至 OpenGL 渲染引擎（若僅 dimAlpha/scalingMode 改變，可即時無損重繪）
        syncRenderState()

        val isVideoFileChanged = oldConfig?.videoFilePath != newConfig.videoFilePath
        val isTrimChanged = oldConfig?.enableTrim != newConfig.enableTrim ||
                oldConfig.trimStartMs != newConfig.trimStartMs ||
                oldConfig.trimEndMs != newConfig.trimEndMs

        if (isVideoFileChanged || isTrimChanged) {
            savedPosition = if (newConfig.enableTrim) newConfig.trimStartMs else 0L
            if (isVisible && isSurfaceAvailable) {
                recreatePlayer()
            }
        } else {
            exoPlayer?.let { player ->
                applyPlayerSettings(player, newConfig)
            }
        }
    }

    private fun syncRenderState() {
        val config = currentConfig ?: return
        glRenderer.updateRenderState(
            WallpaperRenderState(
                surfaceWidth = surfaceWidth,
                surfaceHeight = surfaceHeight,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                scalingMode = config.scalingMode,
                dimAlpha = config.dimAlpha,
                zoomScale = config.zoomScale,
                panOffsetX = config.panOffsetX,
                panOffsetY = config.panOffsetY
            )
        )
    }

    private fun resumeOrInitPlayer() {
        val config = currentConfig ?: return
        if (!config.hasVideo) return

        val player = exoPlayer
        if (player == null) {
            initAndStartPlayer(config)
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(if (config.enableTrim) config.trimStartMs else 0L)
            }
            player.play()
        }
    }

    private fun initAndStartPlayer(config: WallpaperConfig) {
        val file = File(config.videoFilePath)
        if (!file.exists() || file.length() == 0L) return

        val outputSurface = activeOutputSurface ?: return

        val player = ExoPlayer.Builder(context).build().apply {
            setVideoSurface(outputSurface)
            repeatMode = Player.REPEAT_MODE_ONE
            applyPlayerSettings(this, config)

            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        val rotation = videoSize.unappliedRotationDegrees
                        val (w, h) = if (rotation == 90 || rotation == 270) {
                            videoSize.height to videoSize.width
                        } else {
                            videoSize.width to videoSize.height
                        }
                        videoWidth = w
                        videoHeight = h
                        syncRenderState()
                    }
                }
            })

            val mediaItemBuilder = MediaItem.Builder().setUri(file.toURI().toString())
            if (config.enableTrim && (config.trimStartMs > 0L || config.trimEndMs > config.trimStartMs)) {
                val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(config.trimStartMs)
                if (config.trimEndMs > config.trimStartMs) {
                    clipBuilder.setEndPositionMs(config.trimEndMs)
                }
                mediaItemBuilder.setClippingConfiguration(clipBuilder.build())
            }

            setMediaItem(mediaItemBuilder.build())
            prepare()
            if (savedPosition > 0L) {
                seekTo(savedPosition)
            }
            playWhenReady = true
        }

        exoPlayer = player
    }

    private fun applyPlayerSettings(player: ExoPlayer, config: WallpaperConfig) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, config.isMuted)
            .build()

        player.volume = if (config.isMuted) 0f else config.volume.coerceIn(0f, 1f)
        player.playbackParameters = PlaybackParameters(config.playbackSpeed)
    }

    private fun recreatePlayer() {
        releasePlayerInternal(savePosition = false)
        val config = currentConfig ?: return
        if (isVisible && isSurfaceAvailable) {
            initAndStartPlayer(config)
        }
    }

    private fun releasePlayerInternal(savePosition: Boolean) {
        exoPlayer?.let { player ->
            if (savePosition) {
                savedPosition = player.currentPosition
            }
            player.stop()
            player.clearVideoSurface()
            player.release()
        }
        exoPlayer = null
    }

    fun release() {
        deepPowerSavingJob?.cancel()
        glRenderer.release()
        releasePlayerInternal(savePosition = false)
    }
}
