package com.makay.launcher.data

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.makay.launcher.HomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * İsteğe bağlı erişilebilirlik servisi:
 * - Engellenmiş paketler açıldığında ana ekrana döndürüp ebeveyn PIN ister
 * - Misafir kilidinde ayar ekranını engeller (paket bazlı)
 */
class MakayAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        scope.launch {
            val settings = SettingsStore(applicationContext).settings.first()
            if (!settings.tvAccessibilityAssist) return@launch
            val blocked = ParentalGate.isPackageBlocked(settings, pkg)
            val guestBlock = settings.tvGuestLock && pkg.contains("settings", ignoreCase = true)
            if (!blocked && !guestBlock) return@launch
            val intent = Intent(applicationContext, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_PARENTAL_PKG, if (blocked) pkg else "")
                putExtra(EXTRA_GUEST_BLOCK, guestBlock)
            }
            runCatching { startActivity(intent) }
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        const val EXTRA_PARENTAL_PKG = "makay_parental_pkg"
        const val EXTRA_GUEST_BLOCK = "makay_guest_block"
    }
}
