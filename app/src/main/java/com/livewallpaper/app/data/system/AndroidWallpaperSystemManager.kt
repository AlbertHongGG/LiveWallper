package com.livewallpaper.app.data.system

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import com.livewallpaper.app.service.VideoWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Android 原生系統桌布管理器實作。
 * 實現桌布快照擷取、智慧精準還原與原廠出廠預設桌布降級容錯。
 */
class AndroidWallpaperSystemManager(
    private val context: Context
) : WallpaperSystemManager {

    private val wallpaperManager: WallpaperManager by lazy {
        WallpaperManager.getInstance(context)
    }

    private val snapshotDir: File
        get() = File(context.filesDir, "snapshots").apply { if (!exists()) mkdirs() }

    private val snapshotFile: File
        get() = File(snapshotDir, "user_original_bg.png")

    override fun isWallpaperServiceActive(): Boolean {
        return runCatching {
            val currentInfo = wallpaperManager.wallpaperInfo ?: return false
            val myService = ComponentName(context, VideoWallpaperService::class.java)
            currentInfo.component == myService
        }.getOrDefault(false)
    }

    override fun hasSnapshot(): Boolean {
        return snapshotFile.exists() && snapshotFile.length() > 0L
    }

    override suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 若當前已經是本應用的動態桌布，絕不可快照（避免捕獲到影片畫面）
            if (isWallpaperServiceActive()) {
                return@runCatching false
            }

            // 2. 若已經存在快照，保留最初快照，不予覆蓋
            if (hasSnapshot()) {
                return@runCatching true
            }

            // 3. 取得當前系統靜態桌布
            val drawable = wallpaperManager.drawable ?: return@runCatching false

            // 4. 安全轉換為點陣圖 (Bitmap)
            val bitmap: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }

            // 5. 壓縮寫入沙盒私有檔案
            FileOutputStream(snapshotFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            true
        }
    }

    override suspend fun restoreOriginalWallpaper(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 分支 A：若存在備份快照，100% 精準復原使用者原本的照片
            if (hasSnapshot()) {
                val bitmap = BitmapFactory.decodeFile(snapshotFile.absolutePath)
                if (bitmap != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(
                            bitmap,
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                        )
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                    snapshotFile.delete()
                    return@runCatching
                }
            }

            // 分支 B：無快照或解碼失敗時，優雅回退至系統原廠預設桌布
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.clear(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.clear()
            }
            Unit
        }
    }
}
