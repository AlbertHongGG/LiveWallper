package com.livewallpaper.app.presentation.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.preview.MediaTaskUiState
import com.livewallpaper.app.presentation.theme.AppColors

/**
 * 媒體處理與載入任務之獨立懸浮進度面板 (Progress Dock)。
 * 完美解耦網址下載與本地相簿匯入，提供統一、精緻且收斂的進度展示與取消微互動。
 */
@Composable
fun MediaTaskProgressDock(
    taskState: MediaTaskUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = taskState !is MediaTaskUiState.Idle,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val isRemoteDownload = taskState is MediaTaskUiState.RemoteDownloading
        val titleText = if (isRemoteDownload) "正在下載網路影片" else "正在匯入本地影片"
        val subtitleText = when (taskState) {
            is MediaTaskUiState.RemoteDownloading -> {
                if (!taskState.speedText.isNullOrBlank()) {
                    "${taskState.statusMessage} · ${taskState.speedText}"
                } else {
                    taskState.statusMessage
                }
            }
            is MediaTaskUiState.LocalImporting -> taskState.statusMessage
            MediaTaskUiState.Idle -> ""
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Black.copy(alpha = 0.06f),
                    ambientColor = Color.Black.copy(alpha = 0.03f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.surfaceLight)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左側專屬狀態圖示與雙行資訊
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.accentIndigo.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRemoteDownload) Icons.Default.CloudDownload else Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = AppColors.accentIndigo,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = titleText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitleText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.textSecondaryLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 右側精緻且收斂的取消膠囊按鈕（淘汰粗暴突兀的紅色純文字）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消任務",
                            tint = AppColors.textSecondaryLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "取消",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 下方平滑進度條（有具體百分比時顯示進度，準備/分析中時顯示流動脈衝）
                if (taskState is MediaTaskUiState.RemoteDownloading && taskState.progressPercent != null) {
                    LinearProgressIndicator(
                        progress = { (taskState.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = AppColors.accentIndigo,
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    LinearProgressIndicator(
                        color = AppColors.accentIndigo,
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
