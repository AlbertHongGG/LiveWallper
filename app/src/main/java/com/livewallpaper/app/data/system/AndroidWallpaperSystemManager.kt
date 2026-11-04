package com.livewallpaper.app.data.system

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import com.livewallpaper.app.service.VideoWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android 原生系統桌布管理器實作。
 */
class AndroidWallpaperSystemManager(
    private val context: Context
) : WallpaperSystemManager {

    private val wallpaperManager: WallpaperManager by lazy {
        WallpaperManager.getInstance(context)
    }

    override suspend fun resetToSystemDefault(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.clear(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.clear()
            }
            Unit
        }
    }

    override fun isWallpaperServiceActive(): Boolean {
        return runCatching {
            val currentInfo = wallpaperManager.wallpaperInfo ?: return false
            val myService = ComponentName(context, VideoWallpaperService::class.java)
            currentInfo.component == myService
        }.getOrDefault(false)
    }
}
