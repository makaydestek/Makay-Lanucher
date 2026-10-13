package com.makay.launcher.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.makay.launcher.domain.DeviceStats

class DeviceStatsStore(private val context: Context) {
    fun snapshot(): DeviceStats {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = stat.totalBytes / 1_000_000_000f
        val availStorage = stat.availableBytes / 1_000_000_000f

        val am = context.getSystemService(ActivityManager::class.java)
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)

        return DeviceStats(
            batteryPercent = ((level * 100f) / scale).toInt(),
            charging = charging,
            usedStorageGb = (totalStorage - availStorage).coerceAtLeast(0f),
            totalStorageGb = totalStorage,
            availMemGb = mem.availMem / 1_000_000_000f,
            totalMemGb = mem.totalMem / 1_000_000_000f,
        )
    }
}
