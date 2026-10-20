package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class EngineeringEntry(
    val label: String,
    val intent: Intent,
)

object EngineeringMenus {
    private val candidates = listOf(
        "Mediatek" to listOf(
            Intent().setClassName("com.mediatek.tv.setting", "com.mediatek.tv.setting.MainActivity"),
            Intent().setClassName("com.mediatek.wwtv.tvcenter", "com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity"),
        ),
        "AmLogic" to listOf(
            Intent().setClassName("com.droidlogic.tv.settings", "com.droidlogic.tv.settings.TvSettingsActivity"),
            Intent().setClassName("com.droidlogic.app.SystemControlManager", "com.droidlogic.app.SystemControlManager"),
        ),
        "Xiaomi / PatchWall" to listOf(
            Intent().setClassName("com.xiaomi.mitv.settings", "com.xiaomi.mitv.settings.MainActivity"),
            Intent().setClassName("com.mitv.tvhome", "com.mitv.tvhome.MainActivity"),
        ),
        "FengOS" to listOf(
            Intent().setClassName("com.fengos.settings", "com.fengos.settings.MainActivity"),
        ),
        "Android TV Ayarları" to listOf(
            Intent("android.settings.SETTINGS"),
            Intent("android.settings.DEVICE_INFO_SETTINGS"),
            Intent("com.android.tv.settings.MAIN"),
        ),
    )

    fun available(context: Context): List<EngineeringEntry> {
        val pm = context.packageManager
        return candidates.mapNotNull { (label, intents) ->
            val match = intents.firstOrNull { intent ->
                intent.resolveActivity(pm) != null ||
                    (intent.component != null && runCatching {
                        pm.getActivityInfo(intent.component!!, 0)
                    }.isSuccess)
            } ?: return@mapNotNull null
            EngineeringEntry(
                label = label,
                intent = match.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
