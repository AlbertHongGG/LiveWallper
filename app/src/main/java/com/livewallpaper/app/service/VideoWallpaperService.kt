package com.livewallpaper.app.service

import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.livewallpaper.app.LiveWallpaperApplication
import com.livewallpaper.app.service.engine.WallpaperPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {
        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private lateinit var playerController: WallpaperPlayerController

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            playerController = WallpaperPlayerController(
                context = applicationContext,
                engineScope = engineScope
            )

            val app = application as LiveWallpaperApplication
            app.container.wallpaperRepository.wallpaperConfig
                .onEach { config ->
                    playerController.onConfigChanged(config)
                }
                .launchIn(engineScope)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event != null) {
                playerController.onTouchEvent(event)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            playerController.onSurfaceCreated(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            playerController.onVisibilityChanged(visible)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            playerController.onSurfaceDestroyed()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            playerController.release()
            engineScope.cancel()
            super.onDestroy()
        }
    }
}
