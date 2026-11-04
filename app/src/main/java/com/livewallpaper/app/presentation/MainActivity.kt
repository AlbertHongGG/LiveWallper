package com.livewallpaper.app.presentation

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.livewallpaper.app.LiveWallpaperApplication
import com.livewallpaper.app.domain.model.VideoSource
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.presentation.features.editor.WallpaperEditorScreen
import com.livewallpaper.app.presentation.features.home.HomeScreen
import com.livewallpaper.app.presentation.features.settings.SettingsPage
import com.livewallpaper.app.presentation.features.settings.SettingsViewModel
import com.livewallpaper.app.presentation.navigation.components.CupertinoSwipeBackContainer
import com.livewallpaper.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.livewallpaper.app.presentation.navigation.model.AppRoute
import com.livewallpaper.app.presentation.notification.GlobalNotificationOverlay
import com.livewallpaper.app.presentation.notification.NotificationController
import com.livewallpaper.app.presentation.preview.MainUiEffect
import com.livewallpaper.app.presentation.preview.MainViewModel
import com.livewallpaper.app.presentation.theme.AppColors
import com.livewallpaper.app.presentation.theme.LiveWallpaperTheme
import com.livewallpaper.app.service.VideoWallpaperService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        val app = application as LiveWallpaperApplication
        MainViewModel.Factory(
            context = applicationContext,
            wallpaperRepository = app.container.wallpaperRepository,
            processVideoSourceUseCase = app.container.processVideoSourceUseCase,
            applyWallpaperUseCase = app.container.applyWallpaperUseCase,
            resetWallpaperUseCase = app.container.resetWallpaperUseCase
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as LiveWallpaperApplication
        SettingsViewModel.Factory(
            wallpaperRepository = app.container.wallpaperRepository,
            mediaExtractor = app.container.mediaExtractor
        )
    }

    // 監聽系統桌布設定跳轉結果
    private val setWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        mainViewModel.refreshServiceStatus()
        if (mainViewModel.isWallpaperServiceRunning()) {
            NotificationController.showSuccess("動態桌布已成功套用至系統桌面！")
        }
    }

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.effect.collect { effect ->
                    when (effect) {
                        is MainUiEffect.RequestSetWallpaperIntent -> {
                            launchSetWallpaperIntent()
                        }
                        is MainUiEffect.NavigateToEditor -> {
                            // 供全域跳轉使用
                        }
                    }
                }
            }
        }

        handleIncomingIntent(intent)

        setContent {
            LiveWallpaperTheme {
                val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Main) }

                GlobalNotificationOverlay {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppColors.backgroundLight
                    ) {
                        CupertinoSwipeBackContainer(
                            backStack = backStack,
                            onPushRoute = { backStack.add(it) },
                            onPopRoute = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                        ) { route ->
                            val navigator = LocalCupertinoNavigator.current

                            when (route) {
                                is AppRoute.Main -> {
                                    HomeScreen(
                                        viewModel = mainViewModel,
                                        onNavigateToSettings = {
                                            navigator.push(AppRoute.Settings)
                                        },
                                        onOpenEditor = { path ->
                                            navigator.push(AppRoute.Editor(path))
                                        }
                                    )
                                }

                                is AppRoute.Editor -> {
                                    val currentConfig = (mainViewModel.stageState.value as? com.livewallpaper.app.presentation.preview.WallpaperStageState.Active)?.config
                                        ?: mainViewModel.getCurrentConfig()
                                    val isRunning = (mainViewModel.stageState.value as? com.livewallpaper.app.presentation.preview.WallpaperStageState.Active)?.isServiceActive
                                        ?: mainViewModel.isWallpaperServiceRunning()

                                    WallpaperEditorScreen(
                                        videoPath = route.videoPath,
                                        initialConfig = currentConfig,
                                        isServiceActive = isRunning,
                                        onDiscard = {
                                            navigator.pop()
                                        },
                                        onApply = { updatedConfig ->
                                            mainViewModel.applyWallpaper(updatedConfig) {
                                                navigator.pop()
                                            }
                                            if (!isRunning) {
                                                // 若為首次啟用，跳系統對話框後返回時自動 pop
                                                navigator.pop()
                                            }
                                        }
                                    )
                                }

                                is AppRoute.Settings -> {
                                    SettingsPage(
                                        viewModel = settingsViewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshServiceStatus()
        settingsViewModel.refreshStats()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                mainViewModel.updateUrlInput(sharedText)
                mainViewModel.handleVideoSource(
                    source = VideoSource.RemoteShared(sharedText),
                    onReadyForEditor = { path ->
                        // 收到分享自動跳轉至 Editor
                        mainViewModel.refreshServiceStatus()
                    }
                )
            }
        }
    }

    private fun launchSetWallpaperIntent() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                )
            }
            setWallpaperLauncher.launch(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                setWallpaperLauncher.launch(fallbackIntent)
            } catch (_: Exception) {
                NotificationController.showWarning("請前往系統設定手動套用動態桌布")
            }
        }
    }
}
