package com.livewallpaper.app.presentation.components

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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.livewallpaper.app.domain.model.ScalingMode
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewSurface(
    videoFile: File,
    scalingMode: ScalingMode,
    isMuted: Boolean,
    playbackSpeed: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoFile) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val mediaItem = MediaItem.fromUri(videoFile.toURI().toString())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // 動態響應屬性變更
    LaunchedEffect(scalingMode) {
        exoPlayer.videoScalingMode = when (scalingMode) {
            ScalingMode.FILL -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            ScalingMode.FIT -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            ScalingMode.STRETCH -> C.VIDEO_SCALING_MODE_DEFAULT
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, isMuted)
            .build()
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearVideoSurface()
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                resizeMode = when (scalingMode) {
                    ScalingMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ScalingMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ScalingMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            }
        },
        update = { view ->
            view.resizeMode = when (scalingMode) {
                ScalingMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                ScalingMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                ScalingMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
        }
    )
}
