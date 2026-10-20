package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

object StandbyHelper {
    /** Cihazda mümkünse bekleme/dream; yetki yoksa false. */
    fun requestStandby(context: Context): Boolean {
        // 1) Dream / screensaver
        val dream = Intent("android.intent.action.MAIN").apply {
            addCategory("android.intent.category.DREAM")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (dream.resolveActivity(context.packageManager) != null) {
            runCatching { context.startActivity(dream) }
            return true
        }
        // 2) PowerManager reflection (çoğu cihazda reddedilir)
        runCatching {
            val pm = context.getSystemService(PowerManager::class.java)
            val method = PowerManager::class.java.getMethod("goToSleep", Long::class.javaPrimitiveType)
            method.invoke(pm, System.currentTimeMillis())
            return true
        }
        // 3) Kullanıcıyı dream ayarına yönlendir
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        return false
    }
}
