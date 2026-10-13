package com.makay.launcher.data

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Settings

class LauncherActionHandler(private val context: Context) {

    fun openDefaultHomePicker() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSafe(intent)
    }

    fun openDreamSettings() {
        startSafe(Intent("android.settings.DREAM_SETTINGS"))
    }

    fun openUsageAccess() {
        startSafe(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openNotificationAccess() {
        LauncherComponents.enableBadges(context)
        startSafe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openSystemSettings() {
        startSafe(Intent(Settings.ACTION_SETTINGS))
    }

    fun openWifiSettings() {
        startSafe(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun openBluetoothSettings() {
        startSafe(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun expandNotifications() {
        val expanded = expandStatusBar("expandNotificationsPanel") || expandStatusBar("expandSettingsPanel")
        if (!expanded) startSafe(Intent(Settings.ACTION_SETTINGS))
    }

    fun toggleFlashlight(currentState: Boolean, onStateChanged: (Boolean) -> Unit) {
        val camera = context.getSystemService(CameraManager::class.java) ?: return
        val id = camera.cameraIdList.firstOrNull { camId ->
            runCatching {
                camera.getCameraCharacteristics(camId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }.getOrDefault(false)
        } ?: return
        val next = !currentState
        val ok = runCatching { camera.setTorchMode(id, next) }.isSuccess
        if (ok) {
            onStateChanged(next)
        }
    }

    fun startSafe(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun expandStatusBar(methodName: String): Boolean {
        return runCatching {
            val service = context.getSystemService("statusbar")
            val statusBar = Class.forName("android.app.StatusBarManager")
            statusBar.getMethod(methodName).invoke(service)
            true
        }.getOrDefault(false)
    }
}
