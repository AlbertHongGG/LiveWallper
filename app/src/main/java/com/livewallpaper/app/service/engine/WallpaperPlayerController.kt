package com.livewallpaper.app.service.engine

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.livewallpaper.app.domain.model.ScalingMode
import com.livewallpaper.app.domain.model.WallpaperConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class WallpaperPlayerController(
    private val context: Context,
    private val engineScope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var currentSurfaceHolder: SurfaceHolder? = null
    private var currentConfig: WallpaperConfig? = null

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
        currentSurfaceHolder = holder
        isSurfaceAvailable = true
        if (isVisible) {
            resumeOrInitPlayer()
        }
    }

    fun onSurfaceDestroyed() {
        isSurfaceAvailable = false
        currentSurfaceHolder = null
        deepPowerSavingJob?.cancel()
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

        val holder = currentSurfaceHolder ?: return

        val player = ExoPlayer.Builder(context).build().apply {
            setVideoSurfaceHolder(holder)
            repeatMode = Player.REPEAT_MODE_ONE
            applyPlayerSettings(this, config)

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
        player.videoScalingMode = when (config.scalingMode) {
            ScalingMode.FILL -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            ScalingMode.FIT -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            ScalingMode.STRETCH -> C.VIDEO_SCALING_MODE_DEFAULT
        }

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
        currentSurfaceHolder = null
        releasePlayerInternal(savePosition = false)
    }
}
