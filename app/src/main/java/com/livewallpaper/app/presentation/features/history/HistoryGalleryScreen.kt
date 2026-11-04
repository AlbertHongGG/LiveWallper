package com.livewallpaper.app.presentation.features.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.model.WallpaperSourceType
import com.livewallpaper.app.presentation.components.ImmersiveScaffold
import com.livewallpaper.app.presentation.components.NavigationBarsSafeSpacer
import com.livewallpaper.app.presentation.components.PremiumConfigHeader
import com.livewallpaper.app.presentation.components.SwipeToConfirmDrawer
import com.livewallpaper.app.presentation.features.history.components.HistoryGalleryCard
import com.livewallpaper.app.presentation.theme.AppColors

@Composable
fun HistoryGalleryScreen(
    viewModel: HistoryViewModel,
    onSelectHistoryItem: (videoPath: String, sourceType: WallpaperSourceType, sourceUrl: String?, presetConfig: WallpaperConfig) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var itemPendingDelete by remember { mutableStateOf<WallpaperHistoryItem?>(null) }
    var showClearAllDrawer by remember { mutableStateOf(false) }

    // 刪除單一項目確認抽屜
    if (itemPendingDelete != null) {
        val target = itemPendingDelete!!
        SwipeToConfirmDrawer(
            title = "DELETE WALLPAPER",
            sliderTitle = "SLIDE TO DELETE",
            confirmedTitle = "RELEASE TO DELETE",
            onConfirm = {
                viewModel.deleteItem(target)
                itemPendingDelete = null
            },
            onDismiss = { itemPendingDelete = null }
        )
    }

    // 清空歷史確認抽屜
    if (showClearAllDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR ALL HISTORY",
            sliderTitle = "SLIDE TO CLEAR ALL",
            confirmedTitle = "RELEASE TO CLEAR",
            onConfirm = {
                viewModel.clearAll(preserveActive = true)
                showClearAllDrawer = false
            },
            onDismiss = { showClearAllDrawer = false }
        )
    }

    ImmersiveScaffold {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumConfigHeader(
                title = "歷史畫廊",
                subtitle = "WALLPAPER ARCHIVE · ${uiState.totalCount} 項目",
                trailing = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllDrawer = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "清空歷史",
                                tint = AppColors.danger,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.accentIndigo,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                uiState.items.isEmpty() -> {
                    // 空狀態
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.accentIndigo.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = AppColors.accentIndigo,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "尚無歷史桌布典藏",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimaryLight
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "只要正式套用過的桌布，系統將自動以高品質縮圖永久典藏於此，未來可隨時一鍵重新套用。",
                                fontSize = 13.sp,
                                color = AppColors.textSecondaryLight,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.item.id }
                        ) { model ->
                            HistoryGalleryCard(
                                item = model.item,
                                isCurrentlyActive = model.isCurrentlyActive,
                                onClick = {
                                    onSelectHistoryItem(
                                        model.item.videoFilePath,
                                        model.item.sourceType,
                                        model.item.sourceUrl,
                                        model.item.lastAppliedConfig
                                    )
                                },
                                onDelete = {
                                    itemPendingDelete = model.item
                                }
                            )
                        }
                    }
                }
            }

            NavigationBarsSafeSpacer(extraHeight = 16.dp)
        }
    }
}
