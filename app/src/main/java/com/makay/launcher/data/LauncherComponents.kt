package com.makay.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherComponents {
    private const val HOME_ALIAS = "com.makay.launcher.HomeDefault"
    private const val BADGE_SERVICE = "com.makay.launcher.data.BadgeNotificationListener"

    fun enableHomeRole(context: Context) {
        setEnabled(context, HOME_ALIAS, true)
    }

    fun enableBadges(context: Context) {
        setEnabled(context, BADGE_SERVICE, true)
    }

    private fun setEnabled(context: Context, className: String, enabled: Boolean) {
        val manager = context.packageManager
        val component = ComponentName(context.packageName, className)
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            manager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }
}
