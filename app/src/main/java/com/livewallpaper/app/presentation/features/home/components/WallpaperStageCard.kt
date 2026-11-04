package com.livewallpaper.app.presentation.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.components.GfButton
import com.livewallpaper.app.presentation.components.GfButtonVariant
import com.livewallpaper.app.presentation.components.VideoPreviewSurface
import com.livewallpaper.app.presentation.theme.AppColors
import java.io.File

@Composable
fun WallpaperStageCard(
    activeConfig: WallpaperConfig?,
    activeMetadata: VideoMetadata?,
    isServiceActive: Boolean,
    onPickLocalVideo: () -> Unit,
    onOpenEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFile = activeConfig?.videoFilePath?.let { File(it) }
    val hasActiveVideo = activeFile != null && activeFile.exists()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = AppColors.surfaceLight,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        if (!hasActiveVideo) {
            // 空狀態：引導卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickLocalVideo)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(AppColors.accentIndigo.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = AppColors.accentIndigo,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "尚未設定動態桌布",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimaryLight
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "點擊從手機相簿挑選喜愛的影片，\n或在上方貼上 Pinterest 連結即可快速下載解析",
                    fontSize = 13.sp,
                    color = AppColors.textSecondaryLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                GfButton(
                    text = "從相簿選取影片",
                    leadingIcon = Icons.Default.AddPhotoAlternate,
                    onClick = onPickLocalVideo,
                    height = 46.dp
                )
            }
        } else {
            // 運作中桌布預覽卡片
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 上半部：影片即時預覽視窗
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .background(Color(0xFF0F172A))
                ) {
                    VideoPreviewSurface(
                        videoFile = activeFile!!,
                        scalingMode = activeConfig!!.scalingMode,
                        isMuted = true,
                        playbackSpeed = activeConfig.playbackSpeed,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 壓暗遮罩模擬
                    if (activeConfig.dimAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = activeConfig.dimAlpha))
                        )
                    }

                    // 狀態指示膠囊
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) AppColors.success else AppColors.warning)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isServiceActive) "系統桌布運行中" else "尚未設為系統桌布",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // 影片長度與解析度標籤
                    if (activeMetadata != null) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeMetadata.formattedResolution} · ${activeMetadata.formattedDuration}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                // 下半部：操作控制項
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GfButton(
                        text = "調整桌布設定",
                        leadingIcon = Icons.Default.Tune,
                        variant = GfButtonVariant.Primary,
                        onClick = { onOpenEditor(activeFile.absolutePath) },
                        modifier = Modifier.weight(1f),
                        height = 46.dp
                    )

                    GfButton(
                        text = "更換相簿影片",
                        leadingIcon = Icons.Default.AddPhotoAlternate,
                        variant = GfButtonVariant.Secondary,
                        onClick = onPickLocalVideo,
                        modifier = Modifier.weight(1f),
                        height = 46.dp
                    )
                }
            }
        }
    }
}
