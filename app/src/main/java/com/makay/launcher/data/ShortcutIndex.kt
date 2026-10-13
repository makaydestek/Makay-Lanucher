package com.makay.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.AppShortcut

class ShortcutIndex(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun all(apps: List<AppInfo>): List<AppShortcut> {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val users = launcherApps.profiles.ifEmpty { listOf(Process.myUserHandle()) }
        return apps.flatMap { app ->
            users.flatMap { user ->
                val query = LauncherApps.ShortcutQuery()
                    .setPackage(app.packageName)
                    .setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                    )
                runCatching {
                    launcherApps.getShortcuts(query, user).orEmpty().map { shortcut ->
                        AppShortcut(
                            id = shortcut.id,
                            label = shortcut.shortLabel?.toString() ?: shortcut.id,
                            packageName = app.packageName,
                        )
                    }
                }.getOrDefault(emptyList())
            }
        }.distinctBy { "${it.packageName}/${it.id}" }
    }
}
