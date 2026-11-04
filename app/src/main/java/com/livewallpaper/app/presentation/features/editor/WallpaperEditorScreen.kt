package com.livewallpaper.app.presentation.features.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livewallpaper.app.data.metadata.VideoMetadataRetriever
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.components.ImmersiveScaffold
import com.livewallpaper.app.presentation.features.editor.components.EditorSettingsCarousel
import com.livewallpaper.app.presentation.features.editor.components.EditorTopBar
import com.livewallpaper.app.presentation.features.editor.components.PhonePreviewMockup
import com.livewallpaper.app.presentation.features.editor.model.EditorSettingGroup
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorAction
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorUiState
import java.io.File

/**
 * 全新架構動態桌布配置畫面 (Wallpaper Editor Studio)。
 *
 * 核心特色：
 * 1. 單頁零滾動 (Zero-Scroll)：徹底拔除 verticalScroll，全內容在單一畫面完全呈現。
 * 2. 頂部藥丸套用列：右上角整合為「套用設定」按鈕，放棄變更直接右滑返回上一頁。
 * 3. 旋轉木馬設定卡片 (HorizontalPager)：分組式設定卡片，水平左右平滑滑動切換。
 * 4. 桌面級指示器 (Dots Indicator)：宛如主螢幕般的圓點指示器，支援即時高亮與點擊跳轉。
 * 5. 真機即時預覽映射：預覽視窗動態綁定設備真實螢幕比例，徹底解決填充縮放無差異問題。
 */
@Composable
fun WallpaperEditorScreen(
    videoPath: String,
    initialConfig: WallpaperConfig,
    isServiceActive: Boolean,
    onDiscard: () -> Unit = {},
    onApply: (WallpaperConfig) -> Unit
) {
    val videoFile = remember(videoPath) { File(videoPath) }
    var metadata by remember { mutableStateOf<VideoMetadata?>(null) }

    LaunchedEffect(videoFile) {
        metadata = VideoMetadataRetriever.retrieve(videoFile)
    }

    var config by remember(initialConfig) { mutableStateOf(initialConfig) }

    LaunchedEffect(metadata) {
        val duration = metadata?.durationMs ?: 0L
        if (config.trimEndMs == 0L && duration > 0L) {
            config = config.copy(trimEndMs = duration)
        }
    }

    val groups = remember { EditorSettingGroup.entries }
    val pagerState = rememberPagerState(initialPage = 0) { groups.size }

    val uiState = remember(videoFile, metadata, config, isServiceActive, pagerState.currentPage) {
        WallpaperEditorUiState(
            videoFile = videoFile,
            metadata = metadata,
            config = config,
            isServiceActive = isServiceActive,
            currentGroup = groups[pagerState.currentPage]
        )
    }

    val onAction: (WallpaperEditorAction) -> Unit = remember {
        { action ->
            config = when (action) {
                is WallpaperEditorAction.ChangeScalingMode -> config.copy(scalingMode = action.mode)
                is WallpaperEditorAction.ChangeDimAlpha -> config.copy(dimAlpha = action.alpha)
                is WallpaperEditorAction.ChangePlaybackSpeed -> config.copy(playbackSpeed = action.speed)
                is WallpaperEditorAction.ToggleTrim -> config.copy(enableTrim = action.enabled)
                is WallpaperEditorAction.UpdateTrimRange -> config.copy(trimStartMs = action.startMs, trimEndMs = action.endMs)
                is WallpaperEditorAction.ToggleMute -> config.copy(isMuted = action.isMuted)
                is WallpaperEditorAction.ChangeVolume -> config.copy(volume = action.volume)
                is WallpaperEditorAction.ToggleDoubleTapToPause -> config.copy(doubleTapToPause = action.enabled)
            }
        }
    }

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // 1. 頂部操作列：標題 + 右上角「套用」藥丸按鈕
            EditorTopBar(
                isServiceActive = isServiceActive,
                onApply = {
                    val finalConfig = config.copy(
                        videoFilePath = videoFile.absolutePath,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    onApply(finalConfig)
                }
            )

            // 2. 中央手機預覽模型 (自適應權重，徹底杜絕畫面捲動)
            PhonePreviewMockup(
                videoFile = videoFile,
                config = config,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 下方旋轉木馬設定卡片 + 桌面指示器
            EditorSettingsCarousel(
                state = uiState,
                pagerState = pagerState,
                groups = groups,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
    }
}
