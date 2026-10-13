package com.makay.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PackageMonitor(
    context: Context,
    private val scope: CoroutineScope,
    private val onChanged: suspend (packageName: String?) -> Unit,
) : LauncherApps.Callback() {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun start() {
        launcherApps.registerCallback(this)
    }

    fun stop() {
        launcherApps.unregisterCallback(this)
    }

    private fun emit(packageName: String?) {
        scope.launch { onChanged(packageName) }
    }

    override fun onPackageRemoved(packageName: String, user: UserHandle) = emit(packageName)
    override fun onPackageAdded(packageName: String, user: UserHandle) = emit(packageName)
    override fun onPackageChanged(packageName: String, user: UserHandle) = emit(packageName)
    override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
        emit(null)
    override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
        emit(null)
}
