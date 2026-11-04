package com.livewallpaper.app.presentation.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upgrade
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
    viewModel: SettingsViewModel,
    onRequestPermission: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearStagingDrawer by remember { mutableStateOf(false) }
    var showClearArchiveDrawer by remember { mutableStateOf(false) }

    if (showClearStagingDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR STAGING CACHE",
            sliderTitle = "SLIDE TO CLEAR CACHE",
            confirmedTitle = "RELEASE TO CLEAR",
            onConfirm = {
                viewModel.clearStagingCache()
                showClearStagingDrawer = false
            },
            onDismiss = { showClearStagingDrawer = false }
        )
    }

    if (showClearArchiveDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR WALLPAPER ARCHIVE",
            sliderTitle = "SLIDE TO CLEAR ARCHIVE",
            confirmedTitle = "RELEASE TO CLEAR",
            onConfirm = {
                viewModel.clearArchive(preserveActive = true)
                showClearArchiveDrawer = false
            },
            onDismiss = { showClearArchiveDrawer = false }
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

            // 群組一：桌布體驗與系統整合
            SettingsSection(title = "桌布體驗與系統整合") {
                SettingsTile(
                    icon = Icons.Default.TouchApp,
                    title = "雙擊桌面暫停",
                    subtitle = "雙擊空白處暫停播放",
                    trailing = {
                        AppSwitch(
                            checked = uiState.doubleTapToPause,
                            onCheckedChange = { viewModel.toggleDoubleTapToPause(it) }
                        )
                    }
                )

                SettingsDivider()

                SettingsTile(
                    icon = Icons.Default.Security,
                    title = "原桌布備份防護",
                    subtitle = if (uiState.hasBackupPermission) {
                        if (uiState.hasSnapshotBackup) "已受防護 (已備份原桌布快照)" else "已授權 (清空時精準還原原照片)"
                    } else {
                        "尚未授權 (點擊前往系統開啟權限)"
                    },
                    trailing = {
                        if (uiState.hasBackupPermission) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.success,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "前往設定",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.accentIndigo
                            )
                        }
                    },
                    onTap = {
                        if (!uiState.hasBackupPermission) {
                            onRequestPermission()
                        }
                    }
                )
            }

            // 群組二：儲存與空間管理
            SettingsSection(title = "儲存與空間管理") {
                SettingsTile(
                    icon = Icons.Default.CleaningServices,
                    title = "暫存快取容量",
                    subtitle = "${uiState.stagingSizeFormatted} (${uiState.stagingFilesCount} 個檔案)",
                    trailing = {
                        IconButton(
                            onClick = { showClearStagingDrawer = true },
                            enabled = uiState.stagingFilesCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "清空暫存",
                                tint = if (uiState.stagingFilesCount > 0) AppColors.danger else AppColors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                )

                SettingsDivider()

                SettingsTile(
                    icon = Icons.Default.Collections,
                    title = "歷史典藏庫容量",
                    subtitle = "${uiState.archiveSizeFormatted} (${uiState.archiveFilesCount} 支影片)",
                    trailing = {
                        IconButton(
                            onClick = { showClearArchiveDrawer = true },
                            enabled = uiState.archiveFilesCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "清空典藏庫",
                                tint = if (uiState.archiveFilesCount > 0) AppColors.danger else AppColors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                )
            }

            // 群組三：核心引擎與關於
            SettingsSection(title = "核心引擎與關於") {
                SettingsTile(
                    icon = Icons.Default.Upgrade,
                    title = "YT-DLP 解析引擎",
                    subtitle = "點擊檢查線上版本",
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
                    title = "應用程式版本",
                    subtitle = "v1.0.0"
                )
            }

            NavigationBarsSafeSpacer(extraHeight = 36.dp)
        }
    }
}
