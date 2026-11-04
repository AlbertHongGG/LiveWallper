package com.livewallpaper.app.presentation.components

import android.graphics.Color as AndroidColor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.livewallpaper.app.R
import com.livewallpaper.app.domain.model.ScalingMode
import com.livewallpaper.app.domain.model.VideoScalePolicy
import java.io.File

/**
 * 旗艦級硬體加速影片預覽視圖（搭載純幾何縮放佈局引擎）。
 *
 * 核心架構突破：
 * 1. 徹底拔除 AspectRatioFrameLayout 與 MediaCodec 雙重縮放衝突。
 * 2. 採用 VideoScalePolicy 純數學運算，在 Compose layout 層精確計算繪製尺寸與置中位移。
 * 3. 滿版裁切 (FILL) 鎖定原始寬高比，杜絕 Compose AndroidViewHolder 擠壓造成的垂直拉長。
 * 4. 完美支援即時動態靜音、倍速切換與起訖時間軸剪輯循環。
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewSurface(
    videoFile: File,
    scalingMode: ScalingMode,
    isMuted: Boolean,
    playbackSpeed: Float = 1.0f,
    modifier: Modifier = Modifier,
    videoWidth: Int = 0,
    videoHeight: Int = 0,
    zoomScale: Float = 1.0f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    enableTrim: Boolean = false,
    trimStartMs: Long = 0L,
    trimEndMs: Long = 0L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoPath = videoFile.absolutePath

    // 影片真實寬高（優先取傳入之 Metadata，並由 ExoPlayer onVideoSizeChanged 實時動態校正旋轉）
    var resolvedWidth by remember(videoPath) { mutableIntStateOf(videoWidth) }
    var resolvedHeight by remember(videoPath) { mutableIntStateOf(videoHeight) }

    LaunchedEffect(videoWidth, videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            resolvedWidth = videoWidth
            resolvedHeight = videoHeight
        }
    }

    // 快取播放器實例
    val exoPlayer = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val mediaItem = MediaItem.fromUri(videoFile.toURI().toString())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // 監聽影片真實解析度與旋轉度數（校正 90/270 度旋轉）
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val rotation = videoSize.unappliedRotationDegrees
                    val (w, h) = if (rotation == 90 || rotation == 270) {
                        videoSize.height to videoSize.width
                    } else {
                        videoSize.width to videoSize.height
                    }
                    resolvedWidth = w
                    resolvedHeight = h
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
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

    val currentTransform = remember(zoomScale, panOffsetX, panOffsetY) {
        com.livewallpaper.app.domain.model.ViewportTransform(
            zoomScale = zoomScale,
            panOffsetX = panOffsetX,
            panOffsetY = panOffsetY
        )
    }

    // 純粹幾何縮放佈局器容器
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 1. Layout 階段只計算 zoom=1.0 的基礎尺寸，手勢期間 TextureView 尺寸永遠固定（0 次 Relayout）
                .layout { measurable, constraints ->
                    val containerW = constraints.maxWidth
                    val containerH = constraints.maxHeight
                    val baseTransform = VideoScalePolicy.calculateSurfaceTransform(
                        containerWidth = containerW,
                        containerHeight = containerH,
                        videoWidth = resolvedWidth,
                        videoHeight = resolvedHeight,
                        scalingMode = scalingMode,
                        transform = com.livewallpaper.app.domain.model.ViewportTransform.IDENTITY
                    )

                    val placeable = measurable.measure(
                        Constraints.fixed(baseTransform.baseWidth, baseTransform.baseHeight)
                    )

                    layout(containerW, containerH) {
                        placeable.placeRelative(baseTransform.baseOffsetX, baseTransform.baseOffsetY)
                    }
                }
                // 2. Render 階段在 GPU RenderNode 層執行亞像素級平滑縮放與平移，徹底杜絕主執行緒掉幀
                .graphicsLayer {
                    val surfaceTransform = VideoScalePolicy.calculateSurfaceTransform(
                        containerWidth = this.size.width.toInt(),
                        containerHeight = this.size.height.toInt(),
                        videoWidth = resolvedWidth,
                        videoHeight = resolvedHeight,
                        scalingMode = scalingMode,
                        transform = currentTransform
                    )
                    scaleX = surfaceTransform.scale
                    scaleY = surfaceTransform.scale
                    translationX = surfaceTransform.translationX
                    translationY = surfaceTransform.translationY
                }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
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
                        // 讓視圖層永遠填滿經由 Compose 精確計算的 target 尺寸
                        this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                    if (view.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        view.requestLayout()
                    }
                }
            )
        }
    }
}
