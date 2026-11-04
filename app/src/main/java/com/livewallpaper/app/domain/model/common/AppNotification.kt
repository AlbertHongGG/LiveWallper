package com.livewallpaper.app.domain.model.common

import java.util.UUID

enum class NotificationType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val message: String,
    val durationMs: Long = 4000L
)
