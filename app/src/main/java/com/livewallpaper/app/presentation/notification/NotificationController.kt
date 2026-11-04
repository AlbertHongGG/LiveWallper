package com.livewallpaper.app.presentation.notification

import com.livewallpaper.app.domain.model.common.AppNotification
import com.livewallpaper.app.domain.model.common.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 全域浮動通知控制器（頂部通知佇列管理）。
 */
class NotificationController {
    companion object {
        val instance = NotificationController()

        fun showSuccess(message: String, durationMs: Long = 3500L) {
            instance.showSuccess(message, durationMs)
        }

        fun showError(message: String, durationMs: Long = 4500L) {
            instance.showError(message, durationMs)
        }

        fun showWarning(message: String, durationMs: Long = 3500L) {
            instance.showWarning(message, durationMs)
        }

        fun showInfo(message: String, durationMs: Long = 3500L) {
            instance.showInfo(message, durationMs)
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val dismissJobs = ConcurrentHashMap<String, Job>()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private fun addNotification(notification: AppNotification) {
        _notifications.update { current ->
            val list = current.toMutableList()
            list.add(notification)
            while (list.size > 3) {
                val removed = list.removeAt(0)
                dismissJobs.remove(removed.id)?.cancel()
            }
            list
        }

        val job = scope.launch {
            delay(notification.durationMs)
            remove(notification.id)
        }
        dismissJobs[notification.id] = job
    }

    fun remove(id: String) {
        dismissJobs.remove(id)?.cancel()
        _notifications.update { current ->
            current.filter { it.id != id }
        }
    }

    fun showSuccess(message: String, durationMs: Long = 3500L) {
        addNotification(
            AppNotification(
                type = NotificationType.SUCCESS,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showError(message: String, durationMs: Long = 4500L) {
        addNotification(
            AppNotification(
                type = NotificationType.ERROR,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showWarning(message: String, durationMs: Long = 3500L) {
        addNotification(
            AppNotification(
                type = NotificationType.WARNING,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showInfo(message: String, durationMs: Long = 3500L) {
        addNotification(
            AppNotification(
                type = NotificationType.INFO,
                message = message,
                durationMs = durationMs
            )
        )
    }
}
