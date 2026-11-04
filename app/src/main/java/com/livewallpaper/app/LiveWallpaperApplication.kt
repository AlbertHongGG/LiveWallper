package com.livewallpaper.app

import android.app.Application
import com.livewallpaper.app.di.AppContainer

class LiveWallpaperApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
