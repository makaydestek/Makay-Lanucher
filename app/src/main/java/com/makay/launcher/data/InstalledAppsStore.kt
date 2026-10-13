package com.makay.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.makay.launcher.domain.AppCategorizer
import com.makay.launcher.domain.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class InstalledAppsStore(
    private val context: Context,
    private val usageStore: UsageStore,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    suspend fun reload() = withContext(Dispatchers.IO) {
        val users = launcherApps.profiles.ifEmpty { listOf(Process.myUserHandle()) }
        val personal = Process.myUserHandle()
        val usage = usageStore.snapshot()
        // Ağ/disk dışı CPU-yoğun işlemler Default dispatcher'da çalışır; UI jank'ı önlenir
        val loaded = withContext(Dispatchers.Default) {
            users.flatMap { user ->
                val privateSpace = isPrivateSpace(user, personal)
                val work = user != personal && !privateSpace
                launcherApps.getActivityList(null, user).map { info ->
                    val pkg = info.applicationInfo.packageName
                    val isSystem = (info.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    AppInfo(
                        label = info.label?.toString().orEmpty().ifBlank { pkg },
                        packageName = pkg,
                        activityName = info.componentName.className,
                        user = user,
                        category = AppCategorizer.categorize(pkg, isSystem),
                        isSystem = isSystem,
                        usageCount = usage[pkg] ?: 0,
                        work = work,
                        privateSpace = privateSpace,
                    )
                }
            }
                .distinctBy { it.key }
                .sortedBy { it.label.lowercase() }
        }
        _apps.value = loaded
    }

    fun activityInfo(app: AppInfo) =
        launcherApps.getActivityList(app.packageName, app.user)
            .firstOrNull { it.componentName.className == app.activityName }

    fun launch(app: AppInfo) {
        usageStore.record(app.packageName)
        runCatching {
            launcherApps.startMainActivity(
                android.content.ComponentName(app.packageName, app.activityName),
                app.user,
                null,
                null,
            )
        }
    }

    fun launchShortcut(packageName: String, shortcutId: String, user: UserHandle) {
        if (!launcherApps.hasShortcutHostPermission()) return
        runCatching { launcherApps.startShortcut(packageName, shortcutId, null, null, user) }
    }

    private fun isPrivateSpace(user: UserHandle, personal: UserHandle): Boolean {
        if (user == personal || Build.VERSION.SDK_INT < 35) return false
        return runCatching {
            val getType = UserManager::class.java.methods.firstOrNull { method ->
                method.name == "getProfileType" && method.parameterTypes.size == 1
            }
            val type = getType?.invoke(userManager, user) as? String
            type?.contains("private", ignoreCase = true) == true
        }.getOrDefault(false)
    }
}
