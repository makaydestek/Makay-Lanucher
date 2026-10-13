package com.makay.launcher.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

object SystemUsage {
    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun counts(context: Context): Map<String, Int> {
        if (!hasAccess(context)) return emptyMap()
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - 14L * 24 * 60 * 60 * 1000
        return manager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, start, end)
            .orEmpty()
            .associate { it.packageName to (it.totalTimeInForeground / 60_000L).toInt() }
            .filterValues { it > 0 }
    }
}
