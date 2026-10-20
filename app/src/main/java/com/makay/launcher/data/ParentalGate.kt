package com.makay.launcher.data

import com.makay.launcher.domain.LauncherSettings
import com.makay.launcher.domain.TvFamilySlot
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
        val slots = activeFamilySlots(settings)
        if (slots.isNotEmpty()) {
            return slots.any { inWindow(it.startMin, it.endMin) }
        }
        if (!settings.tvParentalSchedule) return true
        return inWindow(settings.tvParentalStartMin, settings.tvParentalEndMin)
    }

    fun activeFamilySlots(settings: LauncherSettings): List<TvFamilySlot> {
        if (settings.tvFamilySlots.isBlank()) return emptyList()
        return settings.tvFamilySlots.split(',')
            .mapNotNull { raw -> runCatching { TvFamilySlot.valueOf(raw.trim()) }.getOrNull() }
    }

    fun isPackageBlocked(settings: LauncherSettings, packageName: String): Boolean {
        if (!isRestrictedNow(settings)) return false
        val blocked = csv(settings.tvBlockedPackages)
        return packageName in blocked
    }

    fun isInputBlocked(settings: LauncherSettings, inputId: String): Boolean {
        if (!isRestrictedNow(settings)) return false
        val blocked = csv(settings.tvBlockedInputs)
        return inputId in blocked
    }

    private fun inWindow(start: Int, end: Int): Boolean {
        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val s = start.coerceIn(0, 24 * 60 - 1)
        val e = end.coerceIn(0, 24 * 60 - 1)
        return if (s <= e) minutes in s until e else minutes >= s || minutes < e
    }

    private fun csv(raw: String): Set<String> =
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}
