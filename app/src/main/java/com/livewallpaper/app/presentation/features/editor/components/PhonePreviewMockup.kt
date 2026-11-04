package com.livewallpaper.app.presentation.features.editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.components.VideoPreviewSurface
import java.io.File

@Composable
fun PhonePreviewMockup(
    videoFile: File,
    config: WallpaperConfig,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    // 動態綁定當前設備螢幕之真實比例 (例如 20:9 或 19.5:9)，終結固定 9:16 造成縮放無差異之問題
    val deviceAspectRatio = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val w = configuration.screenWidthDp.toFloat()
        val h = configuration.screenHeightDp.toFloat()
        if (h > 0f) w / h else (9f / 16f)
    }

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
            Box(modifier = Modifier.fillMaxSize()) {
                if (videoFile.exists()) {
                    VideoPreviewSurface(
                        videoFile = videoFile,
                        scalingMode = config.scalingMode,
                        isMuted = config.isMuted,
                        playbackSpeed = config.playbackSpeed,
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
