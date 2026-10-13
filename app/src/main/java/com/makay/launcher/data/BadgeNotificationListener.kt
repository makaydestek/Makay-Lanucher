package com.makay.launcher.data

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BadgeNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() = publish()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    private fun publish() {
        val counts = activeNotifications
            ?.filter { !it.isOngoing }
            ?.groupingBy { it.packageName }
            ?.eachCount()
            .orEmpty()
        badges.value = counts
    }

    companion object {
        private val badges = MutableStateFlow<Map<String, Int>>(emptyMap())
        val flow: StateFlow<Map<String, Int>> = badges
    }
}
