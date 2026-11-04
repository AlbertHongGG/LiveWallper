package com.livewallpaper.app.presentation.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.presentation.components.AppSwitch
import com.livewallpaper.app.presentation.components.ImmersiveScaffold
import com.livewallpaper.app.presentation.components.NavigationBarsSafeSpacer
import com.livewallpaper.app.presentation.components.PremiumConfigHeader
import com.livewallpaper.app.presentation.components.SwipeToConfirmDrawer
import com.livewallpaper.app.presentation.features.settings.components.SettingsDivider
import com.livewallpaper.app.presentation.features.settings.components.SettingsSection
import com.livewallpaper.app.presentation.features.settings.components.SettingsTile
import com.livewallpaper.app.presentation.theme.AppColors

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SettingsPage(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearCacheDrawer by remember { mutableStateOf(false) }

    if (showClearCacheDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR CACHE",
            sliderTitle = "SLIDE TO CLEAR",
            confirmedTitle = "RELEASE TO CLEAR",
            onConfirm = {
                viewModel.clearCache()
                showClearCacheDrawer = false
            },
            onDismiss = { showClearCacheDrawer = false }
        )
    }

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PremiumConfigHeader(
                title = "系統設定",
                subtitle = "SYSTEM CONFIGURATION"
            )

            SettingsSection(title = "動態桌布與節能") {
                SettingsTile(
                    icon = Icons.Default.TouchApp,
                    iconColor = AppColors.accentIndigo,
                    title = "桌面雙擊暫停",
                    subtitle = "在手機桌面空白處雙擊即可暫停或恢復播放",
                    trailing = {
                        AppSwitch(
                            checked = uiState.doubleTapToPause,
                            onCheckedChange = { viewModel.toggleDoubleTapToPause(it) }
                        )
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.OfflineBolt,
                    iconColor = AppColors.success,
                    title = "深度節能釋放延遲",
                    subtitle = "離開桌面 ${uiState.powerSaveDelaySeconds} 秒後徹底釋放硬體解碼器",
                    trailing = {
                        Text(
                            text = "${uiState.powerSaveDelaySeconds}s",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textSecondaryLight
                        )
                    }
                )
            }

            SettingsSection(title = "儲存與快取管理") {
                SettingsTile(
                    icon = Icons.Default.CleaningServices,
                    iconColor = AppColors.primary,
                    title = "暫存快取容量",
                    subtitle = "${uiState.cacheSizeFormatted} · ${uiState.cacheFilesCount} 個暫存檔案",
                    trailing = {
                        IconButton(
                            onClick = { showClearCacheDrawer = true },
                            enabled = uiState.cacheFilesCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "清空快取",
                                tint = if (uiState.cacheFilesCount > 0) AppColors.danger else AppColors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                )
            }

            SettingsSection(title = "核心引擎與關於") {
                SettingsTile(
                    icon = Icons.Default.Upgrade,
                    iconColor = AppColors.accentIndigo,
                    title = "YT-DLP 核心引擎",
                    subtitle = "點擊以線上檢查並更新解析核心",
                    trailing = {
                        if (uiState.isUpdatingYtDlp) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = AppColors.accentIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = uiState.ytDlpVersion,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.textSecondaryLight
                            )
                        }
                    },
                    onTap = { viewModel.updateYtDlpEngine() }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.Info,
                    iconColor = AppColors.primary,
                    title = "應用程式版本",
                    subtitle = "v1.0.0 (Modern Architecture)"
                )
            }

            NavigationBarsSafeSpacer(extraHeight = 36.dp)
        }
    }
}
