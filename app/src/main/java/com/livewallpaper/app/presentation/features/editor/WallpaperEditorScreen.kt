package com.livewallpaper.app.presentation.features.editor

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.data.metadata.VideoMetadataRetriever
import com.livewallpaper.app.domain.model.ScalingMode
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.components.AppSwitch
import com.livewallpaper.app.presentation.components.GfButton
import com.livewallpaper.app.presentation.components.ImmersiveScaffold
import com.livewallpaper.app.presentation.components.NavigationBarsSafeSpacer
import com.livewallpaper.app.presentation.components.VideoPreviewSurface
import com.livewallpaper.app.presentation.features.editor.components.ProTimelineSlider
import com.livewallpaper.app.presentation.features.editor.components.SlidingSegmentedControl
import com.livewallpaper.app.presentation.features.settings.components.SettingsDivider
import com.livewallpaper.app.presentation.features.settings.components.SettingsSection
import com.livewallpaper.app.presentation.features.settings.components.SettingsTile
import com.livewallpaper.app.presentation.theme.AppColors
import java.io.File

@Composable
fun WallpaperEditorScreen(
    videoPath: String,
    initialConfig: WallpaperConfig,
    isServiceActive: Boolean,
    onDiscard: () -> Unit,
    onApply: (WallpaperConfig) -> Unit
) {
    val videoFile = remember(videoPath) { File(videoPath) }
    var metadata by remember { mutableStateOf<VideoMetadata?>(null) }

    LaunchedEffect(videoFile) {
        metadata = VideoMetadataRetriever.retrieve(videoFile)
    }

    // 編輯中的本機暫態配置
    var scalingMode by remember { mutableStateOf(initialConfig.scalingMode) }
    var horizontalOffset by remember { mutableFloatStateOf(initialConfig.horizontalOffset) }
    var dimAlpha by remember { mutableFloatStateOf(initialConfig.dimAlpha) }
    var isMuted by remember { mutableStateOf(initialConfig.isMuted) }
    var volume by remember { mutableFloatStateOf(initialConfig.volume) }
    var playbackSpeed by remember { mutableFloatStateOf(initialConfig.playbackSpeed) }
    var enableTrim by remember { mutableStateOf(initialConfig.enableTrim) }
    var trimStartMs by remember { mutableLongStateOf(initialConfig.trimStartMs) }
    var trimEndMs by remember {
        mutableLongStateOf(
            if (initialConfig.trimEndMs > 0L) initialConfig.trimEndMs
            else (metadata?.durationMs ?: 0L)
        )
    }

    LaunchedEffect(metadata) {
        if (trimEndMs == 0L && (metadata?.durationMs ?: 0L) > 0L) {
            trimEndMs = metadata!!.durationMs
        }
    }

    ImmersiveScaffold {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 頂部導航列：右側「放棄變更」，支援左緣右滑返回
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "桌布配置",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimaryLight
                    )
                    Text(
                        text = "WALLPAPER STUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textSecondaryLight,
                        letterSpacing = 1.2.sp
                    )
                }

                Text(
                    text = "放棄變更",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textSecondaryLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDiscard)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // 可滾動編輯區塊
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 模擬手機桌面預覽視窗 (9:16 比例)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .aspectRatio(9f / 16f)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color.Black.copy(alpha = 0.16f),
                                ambientColor = Color.Black.copy(alpha = 0.08f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                        color = Color.Black
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (videoFile.exists()) {
                                VideoPreviewSurface(
                                    videoFile = videoFile,
                                    scalingMode = scalingMode,
                                    isMuted = isMuted,
                                    playbackSpeed = playbackSpeed,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // 壓暗遮罩渲染
                            if (dimAlpha > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = dimAlpha))
                                )
                            }

                            // 頂部手機桌面模擬時間
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "12:00",
                                    fontSize = 28.sp,
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

                // 2. 結構化設定區塊：畫面比例與視覺
                SettingsSection(title = "畫面比例與視覺") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "顯示填充模式",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SlidingSegmentedControl(
                            selectedMode = scalingMode,
                            onModeSelected = { scalingMode = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 壓暗遮罩滑桿
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "桌面壓暗遮罩",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimaryLight
                            )
                            Text(
                                text = "${(dimAlpha * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.accentIndigo
                            )
                        }

                        Slider(
                            value = dimAlpha,
                            onValueChange = { dimAlpha = it },
                            valueRange = 0f..0.6f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = AppColors.accentIndigo,
                                inactiveTrackColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                // 3. 結構化設定區塊：片段剪輯與播放
                val durationMs = metadata?.durationMs ?: 0L
                SettingsSection(title = "片段剪輯與播放") {
                    SettingsTile(
                        icon = Icons.Default.Crop,
                        iconColor = AppColors.accentIndigo,
                        title = "啟用片段剪輯",
                        subtitle = if (durationMs > 0L) "限制在指定時間範圍內循環播放" else "正在讀取影片時長...",
                        trailing = {
                            AppSwitch(
                                checked = enableTrim,
                                onCheckedChange = { enableTrim = it },
                                enabled = durationMs > 0L
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = enableTrim && durationMs > 0L,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            ProTimelineSlider(
                                totalDurationMs = durationMs,
                                startMs = trimStartMs,
                                endMs = if (trimEndMs > 0L) trimEndMs else durationMs,
                                onRangeChange = { start, end ->
                                    trimStartMs = start
                                    trimEndMs = end
                                }
                            )
                        }
                    }

                    SettingsDivider()

                    // 播放速度切換
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = AppColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "播放速度",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimaryLight
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            speeds.forEach { speed ->
                                val isSelected = playbackSpeed == speed
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AppColors.accentIndigo else Color(0xFFF1F5F9))
                                        .clickable { playbackSpeed = speed }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else AppColors.textSecondaryLight
                                    )
                                }
                            }
                        }
                    }

                    SettingsDivider()

                    // 音效開關與音量
                    SettingsTile(
                        icon = Icons.Default.VolumeUp,
                        iconColor = if (isMuted) AppColors.secondary else AppColors.accentIndigo,
                        title = "開啟桌布聲音",
                        subtitle = if (isMuted) "預設靜音以省電且不打擾音樂" else "已啟用桌布背景音效",
                        trailing = {
                            AppSwitch(
                                checked = !isMuted,
                                onCheckedChange = { isMuted = !it }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = !isMuted,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "音量大小",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.textSecondaryLight
                                )
                                Text(
                                    text = "${(volume * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.accentIndigo
                                )
                            }
                            Slider(
                                value = volume,
                                onValueChange = { volume = it },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = AppColors.accentIndigo,
                                    inactiveTrackColor = Color(0xFFE2E8F0)
                                )
                            )
                        }
                    }
                }

                NavigationBarsSafeSpacer(extraHeight = 80.dp)
            }

            // 4. 底部固定操作欄：全寬懸浮按鈕
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = AppColors.surfaceLight,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    GfButton(
                        text = if (isServiceActive) "套用桌布設定" else "套用並設為系統桌布",
                        leadingIcon = Icons.Default.Check,
                        onClick = {
                            val updatedConfig = initialConfig.copy(
                                videoFilePath = videoFile.absolutePath,
                                scalingMode = scalingMode,
                                horizontalOffset = horizontalOffset,
                                dimAlpha = dimAlpha,
                                isMuted = isMuted,
                                volume = volume,
                                playbackSpeed = playbackSpeed,
                                enableTrim = enableTrim,
                                trimStartMs = trimStartMs,
                                trimEndMs = trimEndMs,
                                updatedAtMillis = System.currentTimeMillis()
                            )
                            onApply(updatedConfig)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 50.dp
                    )
                }
            }
        }
    }
}
