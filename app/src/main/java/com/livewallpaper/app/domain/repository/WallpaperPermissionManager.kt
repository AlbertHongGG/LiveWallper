package com.livewallpaper.app.domain.repository

import android.content.Intent

/**
 * 桌布備份與系統儲存存取權限管理器介面 (DIP)。
 * 負責檢查是否具備讀取系統原桌布快照的權限，並提供直達系統設定頁的 Intent。
 */
interface WallpaperPermissionManager {
    /**
     * 檢查當前是否已具備備份原桌布所需之系統權限。
     */
    fun hasBackupPermission(): Boolean

    /**
     * 建立導向專屬權限設定頁之 Intent（優先直達此 App 專屬開關頁）。
     */
    fun createRequestPermissionIntent(): Intent
}
