package com.livewallpaper.app.presentation.features.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livewallpaper.app.domain.model.VideoSource
import com.livewallpaper.app.presentation.components.ImmersiveScaffold
import com.livewallpaper.app.presentation.components.NavigationBarsSafeSpacer
import com.livewallpaper.app.presentation.features.home.components.UrlInputField
import com.livewallpaper.app.presentation.features.home.components.WallpaperStageCard
import com.livewallpaper.app.presentation.preview.MainViewModel
import com.livewallpaper.app.presentation.theme.AppColors

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val homeState by viewModel.homeState.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.handleVideoSource(
                source = VideoSource.LocalUri(uri.toString()),
                onReadyForEditor = onOpenEditor
            )
        }
    }

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 頂部標題列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LiveWallpaper",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.textPrimaryLight,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "VIDEO WALLPAPER STUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textSecondaryLight,
                        letterSpacing = 1.5.sp
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "設定",
                        tint = AppColors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pinterest 網址解析輸入框
            UrlInputField(
                urlText = homeState.urlInput,
                onUrlChange = { viewModel.updateUrlInput(it) },
                onSubmitUrl = { url ->
                    viewModel.handleVideoSource(
                        source = VideoSource.RemoteShared(url),
                        onReadyForEditor = onOpenEditor
                    )
                },
                isDownloading = homeState.isDownloading,
                downloadProgress = homeState.downloadProgress,
                downloadStatus = homeState.downloadStatus,
                downloadSpeed = homeState.downloadSpeed,
                onCancelDownload = { viewModel.cancelDownload() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 單一核心主舞台卡片
            WallpaperStageCard(
                activeConfig = homeState.activeConfig,
                activeMetadata = homeState.activeMetadata,
                isServiceActive = homeState.isServiceActive,
                onPickLocalVideo = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onOpenEditor = onOpenEditor
            )

            NavigationBarsSafeSpacer(extraHeight = 32.dp)
        }
    }
}
