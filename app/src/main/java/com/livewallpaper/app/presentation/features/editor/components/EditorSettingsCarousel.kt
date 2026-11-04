package com.livewallpaper.app.presentation.features.editor.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livewallpaper.app.presentation.features.editor.components.groups.AudioInteractionSettingsCard
import com.livewallpaper.app.presentation.features.editor.components.groups.PlaybackSettingsCard
import com.livewallpaper.app.presentation.features.editor.components.groups.VisualSettingsCard
import com.livewallpaper.app.presentation.features.editor.model.EditorSettingGroup
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorAction
import com.livewallpaper.app.presentation.features.editor.model.WallpaperEditorUiState
import kotlinx.coroutines.launch

@Composable
fun EditorSettingsCarousel(
    state: WallpaperEditorUiState,
    pagerState: PagerState,
    groups: List<EditorSettingGroup>,
    onAction: (WallpaperEditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 旋轉木馬卡片區域 (固定高度確保單頁無滾動)
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) { page ->
            when (groups[page]) {
                EditorSettingGroup.VISUAL -> {
                    VisualSettingsCard(
                        config = state.config,
                        onAction = onAction
                    )
                }
                EditorSettingGroup.PLAYBACK -> {
                    PlaybackSettingsCard(
                        config = state.config,
                        durationMs = state.durationMs,
                        onAction = onAction
                    )
                }
                EditorSettingGroup.AUDIO_INTERACTION -> {
                    AudioInteractionSettingsCard(
                        config = state.config,
                        onAction = onAction
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 桌面級分頁指示器 (點點點)
        EditorPageIndicator(
            pageCount = groups.size,
            currentPage = pagerState.currentPage,
            currentGroup = groups[pagerState.currentPage],
            onSelectPage = { targetPage ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        )
    }
}
