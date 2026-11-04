package com.livewallpaper.app.presentation.navigation.contract

import com.livewallpaper.app.presentation.navigation.model.AppRoute

interface CupertinoNavigator {
    fun push(route: AppRoute)
    fun pop()
}
