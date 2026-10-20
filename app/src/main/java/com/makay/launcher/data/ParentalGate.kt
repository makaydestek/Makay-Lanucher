package com.makay.launcher.data

import com.makay.launcher.domain.LauncherSettings
import java.security.MessageDigest
import java.util.Calendar

object ParentalGate {
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(settings: LauncherSettings, pin: String): Boolean {
        if (settings.tvParentalPinHash.isBlank()) return pin.isBlank()
        return settings.tvParentalPinHash == hashPin(pin)
    }

    fun isRestrictedNow(settings: LauncherSettings): Boolean {
        if (!settings.tvParentalEnabled) return false
        if (!settings.tvParentalSchedule) return true
        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = settings.tvParentalStartMin.coerceIn(0, 24 * 60 - 1)
        val end = settings.tvParentalEndMin.coerceIn(0, 24 * 60 - 1)
        return if (start <= end) {
            minutes in start until end
        } else {
            minutes >= start || minutes < end
        }
    }

    fun isPackageBlocked(settings: LauncherSettings, packageName: String): Boolean {
        if (!isRestrictedNow(settings)) return false
        val blocked = settings.tvBlockedPackages.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return packageName in blocked
    }

    fun isInputBlocked(settings: LauncherSettings, inputId: String): Boolean {
        if (!isRestrictedNow(settings)) return false
        val blocked = settings.tvBlockedInputs.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return inputId in blocked
    }
}
