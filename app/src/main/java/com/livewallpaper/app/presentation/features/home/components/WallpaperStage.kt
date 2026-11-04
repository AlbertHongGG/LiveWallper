package com.livewallpaper.app.presentation.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.components.GfButton
import com.livewallpaper.app.presentation.components.VideoPreviewSurface
import com.livewallpaper.app.presentation.preview.WallpaperStageState
import com.livewallpaper.app.presentation.theme.AppColors

/**
 * 旗艦級桌布主舞台元件。
 *
 * 核心特色：
 * 1. 視口權重自適應：填滿中央可用空間，根除下方死白空隙與重心偏上問題。
 * 2. 徹底降噪：拔除所有重複廢話副標。
 * 3. 純粹手勢互動：
 *    - 輕觸預覽視窗 -> 進入編輯設定
 *    - 長按預覽視窗 -> 震動反饋並更換影片
 *    - 右上角叉叉 -> 一鍵清空重設退回空狀態
 *    - 零外掛底部按鈕，100% 畫面完整歸還動態影片呈現。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperStage(
    stageState: WallpaperStageState,
    onPickLocalVideo: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onResetWallpaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        when (stageState) {
            is WallpaperStageState.Empty -> {
                // 1. 空狀態：自適應沉浸引導舞台（無冗餘副標、自然垂直居中）
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = Color.Black.copy(alpha = 0.06f),
                            ambientColor = Color.Black.copy(alpha = 0.03f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = AppColors.surfaceLight,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onPickLocalVideo)
                            .padding(horizontal = 28.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(AppColors.accentIndigo.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = AppColors.accentIndigo,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "尚未設定動態桌布",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimaryLight
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        GfButton(
                            text = "選取相簿影片",
                            leadingIcon = Icons.Default.AddPhotoAlternate,
                            onClick = onPickLocalVideo,
                            height = 48.dp
                        )
                    }
                }
            }

            is WallpaperStageState.Active -> {
                val activeFile = stageState.videoFile
                val config = stageState.config
                val metadata = stageState.metadata
                val isRunning = stageState.isServiceActive
                val haptics = LocalHapticFeedback.current

                // 2. 活躍桌布：100% 畫面完全留給影片呈現（無下方多餘按鈕）
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = Color.Black.copy(alpha = 0.08f),
                            ambientColor = Color.Black.copy(alpha = 0.04f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 零閃爍 TextureView 影片預覽核心 (純幾何縮放引擎)
                        VideoPreviewSurface(
                            videoFile = activeFile,
                            scalingMode = config.scalingMode,
                            zoomScale = config.zoomScale,
                            panOffsetX = config.panOffsetX,
                            panOffsetY = config.panOffsetY,
                            isMuted = true,
                            playbackSpeed = config.playbackSpeed,
                            videoWidth = metadata?.width ?: 0,
                            videoHeight = metadata?.height ?: 0,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 壓暗遮罩層
                        if (config.dimAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = config.dimAlpha))
                            )
                        }

                        // 手勢互動層：單擊進入編輯，長按更換影片
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .combinedClickable(
                                    onClick = { onOpenEditor(activeFile.absolutePath) },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPickLocalVideo()
                                    }
                                )
                        )

                        // 頂部控制列（狀態膠囊 + 重設清空按鈕）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 運行狀態膠囊
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isRunning) AppColors.success else AppColors.warning)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRunning) "運行中" else "未設為系統桌布",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            // 清除 / 重設桌布按鈕（退回空狀態）
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = onResetWallpaper,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清空桌布",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // 底部媒體參數標籤
                        if (metadata != null) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${metadata.formattedResolution} · ${metadata.formattedDuration}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
