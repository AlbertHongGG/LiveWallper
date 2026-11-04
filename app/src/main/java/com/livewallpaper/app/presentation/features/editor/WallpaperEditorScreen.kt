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
 * 全新動態桌布配置畫面 (Wallpaper Studio)。
 *
 * 核心特色：
 * 1. 單頁零滾動 (Zero-Scroll)：無縱向捲動條，所有內容一次性完整呈現。
 * 2. 5 大獨立職責卡片 (SRP)：視覺、剪輯、速度、聲音、互動徹底解耦，杜絕內容擠壓。
 * 3. 連續無縫軌道 + 實體把手：消滅 M3 方形斷軌切口與端點雜點，滑動手感極致絲滑。
 * 4. 無開關專業剪輯工作台：預設全片播放，拖曳即裁切，右上角支援一鍵重設。
 * 5. 一體化音訊調音台：預設靜音，點擊喇叭一鍵靜音/復原，拖曳滑桿即時調節，常駐版面零跳動。
 * 6. 結構化手勢互動卡片：標題副標在左，開關在右，文字極簡降噪。
 * 7. 桌面 Launcher 圓點指示器：移除多餘文字，極致精簡優雅，支援點擊直接跳轉。
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
                is WallpaperEditorAction.UpdateTrimRange -> {
                    val duration = metadata?.durationMs ?: 0L
                    val isTrimmed = action.startMs > 0L || (action.endMs > 0L && duration > 0L && action.endMs < duration)
                    config.copy(
                        enableTrim = isTrimmed,
                        trimStartMs = action.startMs,
                        trimEndMs = action.endMs
                    )
                }
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
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 下方 5 組單一職責旋轉木馬卡片 + 極簡 Launcher 圓點指示器
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
