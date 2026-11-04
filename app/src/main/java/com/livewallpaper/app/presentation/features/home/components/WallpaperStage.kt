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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.components.GfButton
import com.livewallpaper.app.presentation.components.GfButtonVariant
import com.livewallpaper.app.presentation.components.VideoPreviewSurface
import com.livewallpaper.app.presentation.preview.WallpaperStageState
import com.livewallpaper.app.presentation.theme.AppColors

/**
 * 旗艦級桌布主舞台元件。
 * 遵循直式手機視野比例，徹底消除空白留白失衡；
 * 按鈕解耦移出預覽視窗，消除文字折行擠壓與視覺雜亂。
 */
@Composable
fun WallpaperStage(
    stageState: WallpaperStageState,
    onPickLocalVideo: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onResetWallpaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (stageState) {
            is WallpaperStageState.Empty -> {
                // 1. 空狀態：大比例沉浸式引導舞台
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 13f)
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
                            .padding(28.dp),
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "從相簿選取直式影片，\n或在上方貼上 Pinterest 連結立即解析",
                            fontSize = 14.sp,
                            color = AppColors.textSecondaryLight,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
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

                // 2. 活躍桌布：直式手機規格預覽卡片
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 13.5f)
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
                        // 零閃爍 TextureView 影片預覽核心
                        VideoPreviewSurface(
                            videoFile = activeFile,
                            scalingMode = config.scalingMode,
                            isMuted = true,
                            playbackSpeed = config.playbackSpeed,
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

                            // 清除 / 重設桌布按鈕
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

                Spacer(modifier = Modifier.height(18.dp))

                // 3. 獨立操作艙 (Detached Action Dock)
                // 移出影片表面，徹底解決文字折行擠壓與按鈕遮擋影片問題
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GfButton(
                        text = "編輯樣式",
                        leadingIcon = Icons.Default.Tune,
                        variant = GfButtonVariant.Primary,
                        onClick = { onOpenEditor(activeFile.absolutePath) },
                        modifier = Modifier.weight(1f),
                        height = 48.dp
                    )

                    GfButton(
                        text = "更換影片",
                        leadingIcon = Icons.Default.AddPhotoAlternate,
                        variant = GfButtonVariant.Secondary,
                        onClick = onPickLocalVideo,
                        modifier = Modifier.weight(1f),
                        height = 48.dp
                    )
                }
            }
        }
    }
}
