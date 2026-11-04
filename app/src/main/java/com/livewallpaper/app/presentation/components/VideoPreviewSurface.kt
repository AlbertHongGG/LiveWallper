package com.livewallpaper.app.presentation.components

import android.graphics.Color as AndroidColor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.livewallpaper.app.R
import com.livewallpaper.app.domain.model.ScalingMode
import java.io.File

/**
 * 旗艦級硬體加速影片預覽視圖 (TextureView-backed)。
 *
 * 核心架構優勢：
 * 1. 底層強制使用 TextureView（透過 R.layout.view_texture_player 載入），
 *    消除 SurfaceView 造成的非同步穿孔黑屏與重組閃爍問題。
 * 2. 完美支援 Compose 的 RoundedCornerShape 剪裁、陰影與 Alpha 遮罩。
 * 3. 內建透明快門 (Transparent Shutter) 與生命週期感知，進入背景自動暫停，釋放硬體解碼負載。
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewSurface(
    videoFile: File,
    scalingMode: ScalingMode,
    isMuted: Boolean,
    playbackSpeed: Float = 1.0f,
    modifier: Modifier = Modifier,
    enableTrim: Boolean = false,
    trimStartMs: Long = 0L,
    trimEndMs: Long = 0L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoPath = videoFile.absolutePath

    // 依據絕對路徑快取播放器實例，避免多餘重建
    val exoPlayer = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val mediaItem = MediaItem.fromUri(videoFile.toURI().toString())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // 片段剪輯設定動態同步
    LaunchedEffect(enableTrim, trimStartMs, trimEndMs, exoPlayer) {
        if (enableTrim && (trimStartMs > 0L || (trimEndMs > trimStartMs))) {
            val mediaItem = MediaItem.Builder()
                .setUri(videoFile.toURI().toString())
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(trimStartMs)
                        .apply {
                            if (trimEndMs > trimStartMs) {
                                setEndPositionMs(trimEndMs)
                            }
                        }
                        .build()
                )
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            val mediaItem = MediaItem.fromUri(videoFile.toURI().toString())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // 生命週期綁定：進入背景暫停，回到前台恢復
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> exoPlayer.playWhenReady = true
                Lifecycle.Event.ON_STOP -> exoPlayer.playWhenReady = false
                else -> Unit
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // 動態響應縮放模式 (MediaCodec 基礎硬體配置)
    LaunchedEffect(scalingMode, exoPlayer) {
        exoPlayer.videoScalingMode = when (scalingMode) {
            ScalingMode.FILL -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            ScalingMode.FIT -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            ScalingMode.STRETCH -> C.VIDEO_SCALING_MODE_DEFAULT
        }
    }

    // 動態靜音控制
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer.volume = if (isMuted) 0f else 1f
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, isMuted)
            .build()
    }

    // 播放速度控制
    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // 元件銷毀時釋放解碼資源
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearVideoSurface()
            exoPlayer.release()
        }
    }

    val resizeMode = when (scalingMode) {
        ScalingMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        ScalingMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        ScalingMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val playerView = LayoutInflater.from(ctx).inflate(
                R.layout.view_texture_player,
                null,
                false
            ) as PlayerView

            playerView.apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                player = exoPlayer
                setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                this.resizeMode = resizeMode
            }
        },
        update = { view ->
            if (view.player != exoPlayer) {
                view.player = exoPlayer
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
                // 強制觸發內部 AspectRatioFrameLayout 重新測量與佈局
                view.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)?.let { frame ->
                    frame.resizeMode = resizeMode
                    frame.requestLayout()
                }
                view.requestLayout()
                view.invalidate()
            }
        }
    )
}
