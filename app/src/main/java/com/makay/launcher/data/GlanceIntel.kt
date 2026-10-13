package com.makay.launcher.data

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.provider.CalendarContract
import com.makay.launcher.domain.AppCategory
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.GlanceChip
import com.makay.launcher.domain.GlanceSnapshot
import java.util.Calendar
import java.util.Locale

class GlanceIntel(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    fun hasHeadphones(): Boolean {
        val devices = runCatching { audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
        return devices?.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        } == true
    }

    fun registerAudio(onChange: () -> Unit): AudioDeviceCallback {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = onChange()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = onChange()
        }
        audio.registerAudioDeviceCallback(callback, null)
        return callback
    }

    fun unregisterAudio(callback: AudioDeviceCallback) {
        runCatching { audio.unregisterAudioDeviceCallback(callback) }
    }

    fun snapshot(
        apps: List<AppInfo>,
        statsBattery: String,
        weather: String = "",
        headphonesEnabled: Boolean = true,
        calendarEnabled: Boolean = true,
    ): GlanceSnapshot {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val work = weekday in Calendar.MONDAY..Calendar.FRIDAY && hour in 9..17
        val headphones = headphonesEnabled && hasHeadphones()
        val greeting = when (hour) {
            in 5..11 -> "Günaydın"
            in 12..17 -> "İyi günler"
            in 18..21 -> "İyi akşamlar"
            else -> "İyi geceler"
        }
        val event = if (calendarEnabled) nextCalendarEvent() else null
        val chips = when {
            headphones -> apps.filter { it.category == AppCategory.Media }.take(3)
                .map { GlanceChip(it.label, it) }
            work -> apps.filter { it.category == AppCategory.Productivity }.take(3)
                .map { GlanceChip(it.label, it) }
            hour in 5..11 -> apps.filter {
                it.category == AppCategory.Communication || it.packageName.contains("calendar", true)
            }.take(3).map { GlanceChip(it.label, it) }
            else -> apps.sortedByDescending { it.usageCount }.take(3)
                .map { GlanceChip(it.label, it) }
        }
        val headline = when {
            headphones -> "Kulaklık bağlı · müzik"
            work -> "İş saatleri · üretkenlik"
            hour in 5..11 -> event?.let { "Sabah planı" } ?: "Sabah özeti"
            else -> "Bir bakışta"
        }
        val detail = listOfNotNull(
            weather.ifBlank { null },
            event,
            statsBattery,
            if (headphones) "Spotify / müzik kısayolu hazır" else null,
        ).joinToString("  ·  ")
        return GlanceSnapshot(
            greeting = greeting,
            headline = headline,
            detail = detail,
            chips = chips,
            headphonesConnected = headphones,
            workHours = work,
            weather = weather,
        )
    }

    private fun nextCalendarEvent(): String? {
        return try {
            val start = System.currentTimeMillis()
            val end = start + 24 * 60 * 60 * 1000
            val cursor = context.contentResolver.query(
                CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
                    android.content.ContentUris.appendId(this, start)
                    android.content.ContentUris.appendId(this, end)
                }.build(),
                arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN),
                "${CalendarContract.Instances.BEGIN} >= ?",
                arrayOf(start.toString()),
                "${CalendarContract.Instances.BEGIN} ASC",
            )
            cursor.use { c ->
                if (c != null && c.moveToFirst()) {
                    val title = c.getString(0) ?: return null
                    val begin = c.getLong(1)
                    val time = java.text.SimpleDateFormat("HH:mm", Locale("tr")).format(java.util.Date(begin))
                    "Takvim: $title ($time)"
                } else {
                    null
                }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
