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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.livewallpaper.app.presentation.components.WallpaperPermissionBottomSheet
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
            wallpaperRepository = app.container.wallpaperRepository,
            wallpaperPermissionManager = app.container.wallpaperPermissionManager,
            wallpaperSystemManager = app.container.wallpaperSystemManager,
            processVideoSourceUseCase = app.container.processVideoSourceUseCase,
            applyWallpaperUseCase = app.container.applyWallpaperUseCase,
            resetWallpaperUseCase = app.container.resetWallpaperUseCase,
            urlResolver = app.container.sharedUrlResolver
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as LiveWallpaperApplication
        SettingsViewModel.Factory(
            wallpaperRepository = app.container.wallpaperRepository,
            extractorEngineManager = app.container.extractorEngineManager,
            mediaStorageManager = app.container.mediaStorageManager,
            wallpaperPermissionManager = app.container.wallpaperPermissionManager,
            wallpaperSystemManager = app.container.wallpaperSystemManager
        )
    }

    // 監聽系統桌布設定跳轉結果
    private val setWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        mainViewModel.refreshServiceStatus()
        if (mainViewModel.isWallpaperServiceRunning()) {
            NotificationController.showSuccess("動態桌布已成功套用至系統桌面！")
            mainViewModel.notifyApplySuccess()
        }
    }

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        setContent {
            LiveWallpaperTheme {
                val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Main) }
                val showPermissionPrompt by mainViewModel.showPermissionPrompt.collectAsState()

                LaunchedEffect(Unit) {
                    mainViewModel.effect.collect { effect ->
                        when (effect) {
                            is MainUiEffect.RequestSetWallpaperIntent -> {
                                launchSetWallpaperIntent()
                            }
                            is MainUiEffect.ApplyWallpaperSuccess -> {
                                if (backStack.lastOrNull() is AppRoute.Editor) {
                                    backStack.removeAt(backStack.size - 1)
                                }
                            }
                            is MainUiEffect.NavigateToEditor -> {
                                backStack.add(AppRoute.Editor(effect.filePath))
                            }
                        }
                    }
                }

                if (showPermissionPrompt) {
                    WallpaperPermissionBottomSheet(
                        onGrantClick = {
                            try {
                                startActivity(mainViewModel.requestPermissionIntent())
                            } catch (_: Exception) {
                                NotificationController.showError("無法開啟權限設定頁面")
                            }
                        },
                        onDismiss = {
                            mainViewModel.dismissPermissionPrompt(proceedWithoutBackup = true)
                        }
                    )
                }

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
                                        }
                                    )
                                }

                                is AppRoute.Settings -> {
                                    SettingsPage(
                                        viewModel = settingsViewModel,
                                        onRequestPermission = {
                                            try {
                                                startActivity(mainViewModel.requestPermissionIntent())
                                            } catch (_: Exception) {
                                                NotificationController.showError("無法開啟權限設定頁面")
                                            }
                                        }
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
        mainViewModel.checkAndCaptureSnapshot()
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
                    source = VideoSource.RemoteShared(mainViewModel.urlInput.value)
                )
                intent.removeExtra(Intent.EXTRA_TEXT)
                intent.action = null
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
