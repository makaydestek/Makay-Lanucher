package com.makay.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Açılışta TV autostart hedefini işaretler; asıl launch [HomeActivity] üzerinde yapılır
 * ki launcher hazır olmadan uygulama açılmasın.
 */
class BootAutostartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(context).settings.first()
                if (settings.edition.name == "TV" && settings.tvAutostartKey.isNotBlank()) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_PENDING, true)
                        .apply()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val PREFS = "tv_boot"
        const val KEY_PENDING = "autostart_pending"

        fun consumePending(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val pending = prefs.getBoolean(KEY_PENDING, false)
            if (pending) prefs.edit().putBoolean(KEY_PENDING, false).apply()
            return pending
        }
    }
}
