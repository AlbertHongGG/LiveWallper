package com.livewallpaper.app.presentation.features.editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.VideoScalePolicy
import com.livewallpaper.app.domain.model.ViewportTransform
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.components.VideoPreviewSurface
import java.io.File

@Composable
fun PhonePreviewMockup(
    videoFile: File,
    config: WallpaperConfig,
    metadata: VideoMetadata? = null,
    onTransformChanged: (zoom: Float, panX: Float, panY: Float) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    // 動態綁定當前設備螢幕之真實比例 (例如 20:9 或 19.5:9)
    val deviceAspectRatio = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val w = configuration.screenWidthDp.toFloat()
        val h = configuration.screenHeightDp.toFloat()
        if (h > 0f) w / h else (9f / 16f)
    }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    val currentConfig by rememberUpdatedState(config)
    val currentMetadata by rememberUpdatedState(metadata)
    val currentOnTransformChanged by rememberUpdatedState(onTransformChanged)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(deviceAspectRatio, matchHeightConstraintsFirst = true)
                .padding(vertical = 4.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.16f),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .onSizeChanged { size ->
                        containerWidthPx = size.width.toFloat()
                        containerHeightPx = size.height.toFloat()
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (containerWidthPx <= 0f || containerHeightPx <= 0f) return@detectTransformGestures

                            val cfg = currentConfig
                            val meta = currentMetadata
                            val vWidth = meta?.width ?: 0
                            val vHeight = meta?.height ?: 0

                            val currentTransform = ViewportTransform(
                                zoomScale = cfg.zoomScale,
                                panOffsetX = cfg.panOffsetX,
                                panOffsetY = cfg.panOffsetY
                            )

                            val nextTransform = VideoScalePolicy.applyGestureDelta(
                                current = currentTransform,
                                containerWidth = containerWidthPx,
                                containerHeight = containerHeightPx,
                                videoWidth = vWidth,
                                videoHeight = vHeight,
                                scalingMode = cfg.scalingMode,
                                zoomChange = zoom,
                                panChangeX = pan.x,
                                panChangeY = pan.y
                            )

                            currentOnTransformChanged(
                                nextTransform.zoomScale,
                                nextTransform.panOffsetX,
                                nextTransform.panOffsetY
                            )
                        }
                    }
            ) {
                if (videoFile.exists()) {
                    VideoPreviewSurface(
                        videoFile = videoFile,
                        scalingMode = config.scalingMode,
                        zoomScale = config.zoomScale,
                        panOffsetX = config.panOffsetX,
                        panOffsetY = config.panOffsetY,
                        isMuted = config.isMuted,
                        playbackSpeed = config.playbackSpeed,
                        videoWidth = metadata?.width ?: 0,
                        videoHeight = metadata?.height ?: 0,
                        enableTrim = config.enableTrim,
                        trimStartMs = config.trimStartMs,
                        trimEndMs = config.trimEndMs,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 壓暗遮罩即時呈現
                if (config.dimAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = config.dimAlpha))
                    )
                }

                // 頂部手機桌面模擬時間與說明
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "12:00",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "桌面預覽效果",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
