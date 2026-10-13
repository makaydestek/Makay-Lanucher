package com.makay.launcher

import android.app.Application
import android.content.ComponentCallbacks2
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath
import com.google.android.material.color.DynamicColors
import com.makay.launcher.data.BackupRepository
import com.makay.launcher.data.CrashLogger
import com.makay.launcher.data.DeviceStatsStore
import com.makay.launcher.data.FavoritesStore
import com.makay.launcher.data.GlanceIntel
import com.makay.launcher.data.IconCache
import com.makay.launcher.data.IconPackEngine
import com.makay.launcher.data.InstalledAppsStore
import com.makay.launcher.data.SearchEngine
import com.makay.launcher.data.SettingsStore
import com.makay.launcher.data.ShortcutIndex
import com.makay.launcher.data.AppIconFetcher
import com.makay.launcher.data.TvInputStore
import com.makay.launcher.data.UsageStore
import com.makay.launcher.data.WallpaperFetcher
import com.makay.launcher.data.WallpaperPalette
import com.makay.launcher.data.WeatherStore
import com.makay.launcher.data.WidgetController
import com.makay.launcher.data.WorkspaceStore

class MakayApp : Application(), SingletonImageLoader.Factory {
    lateinit var usageStore: UsageStore
        private set
    lateinit var appsStore: InstalledAppsStore
        private set
    lateinit var favoritesStore: FavoritesStore
        private set
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var iconPackEngine: IconPackEngine
        private set
    lateinit var iconCache: IconCache
        private set
    lateinit var searchEngine: SearchEngine
        private set
    lateinit var deviceStatsStore: DeviceStatsStore
        private set
    lateinit var wallpaperFetcher: WallpaperFetcher
        private set
    lateinit var widgetController: WidgetController
        private set
    lateinit var tvInputStore: TvInputStore
        private set
    lateinit var glanceIntel: GlanceIntel
        private set
    lateinit var shortcutIndex: ShortcutIndex
        private set
    lateinit var backupRepository: BackupRepository
        private set
    lateinit var workspaceStore: WorkspaceStore
        private set
    lateinit var weatherStore: WeatherStore
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
        WallpaperPalette.start(this)
        usageStore = UsageStore(this)
        appsStore = InstalledAppsStore(this, usageStore)
        favoritesStore = FavoritesStore(this)
        settingsStore = SettingsStore(this)
        iconPackEngine = IconPackEngine(this)
        iconCache = IconCache(this, iconPackEngine)
        searchEngine = SearchEngine(this)
        deviceStatsStore = DeviceStatsStore(this)
        wallpaperFetcher = WallpaperFetcher(this)
        widgetController = WidgetController(this)
        tvInputStore = TvInputStore(this)
        glanceIntel = GlanceIntel(this)
        shortcutIndex = ShortcutIndex(this)
        workspaceStore = WorkspaceStore(this)
        weatherStore = WeatherStore(this)
        backupRepository = BackupRepository(this, settingsStore, favoritesStore, widgetController, workspaceStore)
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder().maxSizePercent(context, 0.18).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("icon-cache").toOkioPath())
                    .maxSizeBytes(24L * 1024L * 1024L)
                    .build()
            }
            .crossfade(false)
            .components {
                add(AppIconFetcher.Factory(iconCache, appsStore))
            }
            .build()

    override fun onTrimMemory(level: Int) {
        iconCache.trim(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            iconPackEngine.evict()
        }
        super.onTrimMemory(level)
    }
}
