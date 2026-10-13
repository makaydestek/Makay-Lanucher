package com.makay.launcher.ui

import android.app.Application
import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.media.AudioDeviceCallback
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.makay.launcher.BuildConfig
import com.makay.launcher.data.LauncherActionHandler
import com.makay.launcher.data.LauncherComponents
import com.makay.launcher.data.TvChannelPublisher
import com.makay.launcher.data.UpdateCheckResult
import com.makay.launcher.data.UpdateChecker
import com.makay.launcher.data.UpdateManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makay.launcher.MakayApp
import com.makay.launcher.data.BadgeNotificationListener
import com.makay.launcher.data.PackageMonitor
import com.makay.launcher.data.FormFactorDetector
import com.makay.launcher.domain.AppEdition
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.AppShortcut
import com.makay.launcher.data.WallpaperPalette
import com.makay.launcher.domain.EditionProfile
import com.makay.launcher.domain.FormFactor
import com.makay.launcher.domain.GestureAction
import com.makay.launcher.domain.GlanceSnapshot
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.InputSource
import com.makay.launcher.domain.LauncherSettings
import com.makay.launcher.domain.Overlay
import com.makay.launcher.domain.forEdition
import com.makay.launcher.data.WorkspaceEngine
import com.makay.launcher.domain.PinnedWidget
import com.makay.launcher.domain.WidgetOffer
import com.makay.launcher.domain.WorkspaceItem
import com.makay.launcher.domain.WorkspaceType
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MakayApp
    private val actionHandler = LauncherActionHandler(application)
    private val updateManager = UpdateManager(application)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private val monitor = PackageMonitor(application, viewModelScope) { pkg ->
        pkg?.let {
            app.iconCache.evict(it)
            prunePackage(it)
        }
        app.appsStore.reload()
    }
    private var searchJob: Job? = null
    private var audioCallback: AudioDeviceCallback? = null
    var authenticate: ((String, (Boolean) -> Unit) -> Unit)? = null
    @Volatile private var cachedShortcuts: List<AppShortcut> = emptyList()
    @Volatile private var publishedChannelKey: String? = null
    private var syncedEdition: AppEdition? = null
    private var updateChecked = false

    init {
        monitor.start()
        viewModelScope.launch { app.appsStore.reload() }
        viewModelScope.launch {
            combine(
                app.appsStore.apps,
                app.favoritesStore.homeKeys,
                app.favoritesStore.dockKeys,
                app.favoritesStore.hiddenKeys,
                app.settingsStore.settings,
            ) { apps, homeKeys, dockKeys, hidden, settings ->
                Quintuple(apps, homeKeys, dockKeys, hidden, settings)
            }
                .distinctUntilChanged()
                .conflate()
                .collect { (apps, homeKeys, dockKeys, hidden, settings) ->
                val resolved = resolveEdition(settings)
                val profile = EditionProfile.of(resolved)
                val visible = apps.filter { it.packageName !in hidden }
                val vault = if (profile.vault) apps.filter { it.packageName in hidden } else emptyList()
                val mappedHome = homeKeys.mapNotNull { key -> visible.find { it.key == key } }
                val mappedDock = if (profile.dock) {
                    dockKeys.mapNotNull { key -> visible.find { it.key == key } }
                        .ifEmpty { defaultDock(visible) }
                } else {
                    emptyList()
                }
                if (profile.dock && dockKeys.isEmpty()) {
                    app.favoritesStore.seedDockIfEmpty(mappedDock.map { it.key })
                }
                _state.update {
                    it.copy(
                        apps = visible,
                        favorites = mappedHome,
                        dock = mappedDock.take(if (_state.value.isWideScreen) 7 else 5),
                        hidden = if (profile.vault) hidden else emptySet(),
                        vaultApps = vault,
                        settings = settings.copy(edition = resolved),
                        profile = profile,
                    )
                }
                if (syncedEdition != resolved) {
                    syncedEdition = resolved
                    syncRuntime(profile)
                    viewModelScope.launch(Dispatchers.IO) { reloadCatalogs() }
                }
                refreshGlance(visible)
                if ((!settings.onboardingDone || settings.edition == AppEdition.UNSET) &&
                    _state.value.overlay == Overlay.None
                ) {
                    _state.update { it.copy(overlay = Overlay.Onboarding) }
                }
                if (settings.digitalDetox) {
                    _state.update { it.copy(badges = emptyMap()) }
                }
                if (profile.workspaceGrid) {
                    viewModelScope.launch {
                        app.workspaceStore.migrateIfEmpty(homeKeys, settings.gridColumns)
                    }
                }
                viewModelScope.launch(Dispatchers.IO) {
                    cachedShortcuts = app.shortcutIndex.all(visible)
                    if (profile.isTv) {
                        val channelKey = visible.joinToString(",") { "${it.key}:${it.usageCount}" }
                        if (channelKey != publishedChannelKey) {
                            publishedChannelKey = channelKey
                            TvChannelPublisher(getApplication()).publish(
                                visible.sortedByDescending { it.usageCount }.ifEmpty { visible },
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            BadgeNotificationListener.flow.collect { badges ->
                if (_state.value.profile.notificationBadges && !_state.value.settings.digitalDetox) {
                    _state.update { it.copy(badges = badges) }
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                refreshStats()
                delay(30_000)
            }
        }
        refreshDefaultStatus()
        refreshFormFactor()
        app.widgetController.onPinned = { widget ->
            if (_state.value.profile.widgets) {
                viewModelScope.launch {
                    app.widgetController.persist(widget)
                    placeWidget(widget)
                }
            }
        }
        viewModelScope.launch {
            app.widgetController.widgets.collect { widgets ->
                if (_state.value.profile.widgets) {
                    _state.update { it.copy(widgets = widgets) }
                }
            }
        }
        viewModelScope.launch {
            app.workspaceStore.items.collect { items ->
                val apps = _state.value.apps
                val mapped = items.filter { it.type == WorkspaceType.APP }
                    .mapNotNull { item -> apps.find { it.key == item.appKey } }
                _state.update {
                    it.copy(
                        workspace = items,
                        favorites = mapped.ifEmpty { it.favorites },
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (_state.value.profile.isChosen) reloadCatalogs()
        }
        viewModelScope.launch {
            app.favoritesStore.lockedKeys.collect { locked ->
                _state.update { it.copy(locked = locked) }
            }
        }
        viewModelScope.launch {
            WallpaperPalette.generation.collect { gen ->
                _state.update { it.copy(wallpaperGeneration = gen) }
            }
        }
    }

    fun refreshStats() {
        _state.update { it.copy(deviceStats = app.deviceStatsStore.snapshot()) }
        refreshGlance()
    }

    fun refreshGlance(apps: List<AppInfo> = _state.value.apps) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = _state.value.profile
            val battery = "Pil %${_state.value.deviceStats.batteryPercent}"
            val glance = when {
                profile.glanceSmart -> {
                    val weather = if (profile.weather) app.weatherStore.current() else ""
                    app.glanceIntel.snapshot(
                        apps = apps,
                        statsBattery = battery,
                        weather = weather,
                        headphonesEnabled = profile.headphones,
                        calendarEnabled = profile.calendarGlance,
                    )
                }
                profile.isTv -> GlanceSnapshot(
                    greeting = "İyi seyirler",
                    headline = "TV Sürümü",
                    detail = battery,
                )
                else -> GlanceSnapshot()
            }
            val detox = _state.value.settings.digitalDetox
            _state.update {
                it.copy(glance = if (detox) glance.copy(chips = emptyList(), headline = "Odak modu") else glance)
            }
        }
    }

    fun setGestureNavigation(enabled: Boolean) {
        _state.update { it.copy(gestureNavigation = enabled) }
    }

    fun refreshDefaultStatus() {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, 0)
        _state.update {
            it.copy(isDefaultLauncher = resolved?.activityInfo?.packageName == context.packageName)
        }
    }

    fun perform(action: GestureAction) {
        if (!_state.value.profile.gestures && action != GestureAction.None &&
            action != GestureAction.Settings && action != GestureAction.Search
        ) {
            return
        }
        when (action) {
            GestureAction.None -> Unit
            GestureAction.Drawer -> open(Overlay.Drawer)
            GestureAction.Search -> open(Overlay.Search)
            GestureAction.ControlCenter -> open(Overlay.ControlCenter)
            GestureAction.Notifications -> expandNotifications()
            GestureAction.Settings -> open(Overlay.Settings)
        }
    }

    fun open(overlay: Overlay) {
        if (!overlayAllowed(overlay)) return
        _state.update { it.copy(overlay = overlay, shutterApp = null) }
    }

    fun closeOverlays() {
        _state.update {
            it.copy(
                overlay = Overlay.None,
                shutterApp = null,
                searchQuery = "",
                searchResults = emptyList(),
                vaultUnlocked = false,
                openFolderId = null,
                selectedItemId = null,
            )
        }
    }

    fun launch(appInfo: AppInfo) {
        if (appInfo.packageName in _state.value.locked && _state.value.profile.biometric) {
            authenticate?.invoke("${appInfo.label} kilitli") { ok ->
                if (ok) completeLaunch(appInfo)
            } ?: completeLaunch(appInfo)
            return
        }
        completeLaunch(appInfo)
    }

    private fun completeLaunch(appInfo: AppInfo) {
        runCatching { app.appsStore.launch(appInfo) }
        closeOverlays()
        viewModelScope.launch {
            delay(300)
            app.appsStore.reload()
        }
    }

    fun toggleLocked(appInfo: AppInfo) {
        if (!_state.value.profile.biometric) return
        viewModelScope.launch { app.favoritesStore.toggleLocked(appInfo.packageName) }
    }

    fun openVault() {
        if (!_state.value.profile.vault) return
        authenticate?.invoke("Gizli kasa") { ok ->
            if (ok) _state.update { it.copy(overlay = Overlay.Vault, vaultUnlocked = true) }
        } ?: _state.update { it.copy(overlay = Overlay.Vault, vaultUnlocked = true) }
    }

    fun openShutter(appInfo: AppInfo) {
        val shortcuts = loadShortcuts(appInfo)
        _state.update { it.copy(shutterApp = appInfo, shortcuts = shortcuts) }
    }

    fun launchShortcut(appInfo: AppInfo, shortcut: AppShortcut) {
        app.appsStore.launchShortcut(shortcut.packageName, shortcut.id, appInfo.user)
        closeOverlays()
    }

    fun pinShortcut(appInfo: AppInfo, shortcut: AppShortcut) {
        if (!_state.value.profile.workspaceGrid) return
        viewModelScope.launch {
            app.workspaceStore.update { items ->
                if (items.any { it.shortcutId == shortcut.id && it.appKey == appInfo.key }) return@update items
                val slot = nextSlot(items, 1, 1) ?: return@update items
                items + WorkspaceItem(
                    id = UUID.randomUUID().toString(),
                    type = WorkspaceType.APP,
                    page = slot.first,
                    cellX = slot.second,
                    cellY = slot.third,
                    appKey = appInfo.key,
                    title = shortcut.label,
                    shortcutId = shortcut.id,
                )
            }
        }
        closeOverlays()
        toast("${shortcut.label} ana ekrana eklendi")
    }

    fun setFolderColor(folderId: String, color: Int) {
        if (!_state.value.profile.folders) return
        viewModelScope.launch {
            app.workspaceStore.update { items ->
                items.map { if (it.id == folderId && it.type == WorkspaceType.FOLDER) it.copy(color = color) else it }
            }
        }
    }

    fun addFolderMember(folderId: String, appKey: String) {
        if (!_state.value.profile.folders) return
        viewModelScope.launch {
            app.workspaceStore.update { items ->
                items.map { item ->
                    if (item.id != folderId || item.type != WorkspaceType.FOLDER) item
                    else item.copy(members = (item.members + appKey).distinct())
                }.filterNot { it.type == WorkspaceType.APP && it.appKey == appKey }
            }
        }
    }

    fun toggleFavorite(appInfo: AppInfo) {
        if (!_state.value.profile.isChosen) return
        viewModelScope.launch {
            app.favoritesStore.toggleHome(appInfo.key)
            if (!_state.value.profile.workspaceGrid) return@launch
            app.workspaceStore.update { items ->
                val existing = items.find { it.type == WorkspaceType.APP && it.appKey == appInfo.key }
                if (existing != null) {
                    items.filter { it.id != existing.id }
                } else {
                    val slot = nextSlot(items, 1, 1) ?: return@update items
                    items + WorkspaceItem(
                        id = UUID.randomUUID().toString(),
                        type = WorkspaceType.APP,
                        page = slot.first,
                        cellX = slot.second,
                        cellY = slot.third,
                        appKey = appInfo.key,
                    )
                }
            }
        }
    }

    fun dropItem(itemId: String, page: Int, cellX: Int, cellY: Int) {
        if (!_state.value.profile.workspaceGrid) return
        viewModelScope.launch {
            val settings = _state.value.settings
            app.workspaceStore.update { items ->
                WorkspaceEngine.drop(
                    items, itemId, page, cellX, cellY,
                    columns(), settings.gridRows,
                )
            }
        }
    }

    fun openFolder(id: String) {
        if (!_state.value.profile.folders) return
        _state.update { it.copy(overlay = Overlay.Folder, openFolderId = id) }
    }

    fun selectItem(id: String?) {
        _state.update { it.copy(selectedItemId = id) }
    }

    fun resizeSelected(deltaX: Int, deltaY: Int) {
        if (!_state.value.profile.widgets) return
        val id = _state.value.selectedItemId ?: return
        viewModelScope.launch {
            app.workspaceStore.update { items ->
                items.map { item ->
                    if (item.id != id || item.type != WorkspaceType.WIDGET) item
                    else {
                        val spanX = (item.spanX + deltaX).coerceIn(1, columns())
                        val spanY = (item.spanY + deltaY).coerceIn(1, _state.value.settings.gridRows)
                        if (WorkspaceEngine.fits(
                                items, item.page, item.cellX, item.cellY, spanX, spanY, item.id,
                                columns(), _state.value.settings.gridRows,
                            )
                        ) {
                            item.copy(spanX = spanX, spanY = spanY)
                        } else item
                    }
                }
            }
        }
    }

    fun removeWorkspaceItem(id: String) {
        viewModelScope.launch {
            val item = _state.value.workspace.find { it.id == id }
            if (item?.type == WorkspaceType.WIDGET && item.widgetId > 0) {
                app.widgetController.remove(PinnedWidget(item.widgetId, item.provider))
            }
            app.workspaceStore.update { items -> items.filter { it.id != id } }
            _state.update { it.copy(selectedItemId = null) }
        }
    }

    fun finishOnboarding() {
        if (_state.value.settings.edition == AppEdition.UNSET) return
        _state.update { it.copy(settings = it.settings.copy(onboardingDone = true)) }
        updateSettings { it.copy(onboardingDone = true) }
        LauncherComponents.enableHomeRole(getApplication())
        closeOverlays()
        updateChecked = false
        checkForUpdate(force = false)
    }

    fun selectEdition(edition: AppEdition) {
        if (edition == AppEdition.UNSET) return
        updateSettings { it.forEdition(edition) }
        viewModelScope.launch { pruneStaleItems() }
    }

    fun renameFolder(id: String, title: String) {
        if (!_state.value.profile.folders) return
        viewModelScope.launch {
            app.workspaceStore.update { items ->
                items.map { if (it.id == id && it.type == WorkspaceType.FOLDER) it.copy(title = title) else it }
            }
        }
    }

    fun removeFolderMember(folderId: String, appKey: String) {
        if (!_state.value.profile.folders) return
        viewModelScope.launch {
            app.workspaceStore.update { items ->
                val folder = items.find { it.id == folderId } ?: return@update items
                val members = folder.members.filter { it != appKey }
                val without = items.map {
                    if (it.id == folderId) it.copy(members = members) else it
                }.filter { it.type != WorkspaceType.FOLDER || it.members.isNotEmpty() || it.id != folderId }
                if (members.isEmpty()) {
                    _state.update { it.copy(overlay = Overlay.None, openFolderId = null) }
                    return@update without
                }
                val slot = nextSlot(without, 1, 1) ?: return@update without
                without + WorkspaceItem(
                    id = UUID.randomUUID().toString(),
                    type = WorkspaceType.APP,
                    page = slot.first,
                    cellX = slot.second,
                    cellY = slot.third,
                    appKey = appKey,
                )
            }
        }
    }

    fun setGrid(columns: Int? = null, rows: Int? = null) {
        viewModelScope.launch {
            var nextCols = _state.value.settings.gridColumns
            var nextRows = _state.value.settings.gridRows
            app.settingsStore.update { current ->
                nextCols = columns ?: current.gridColumns
                nextRows = rows ?: current.gridRows
                current.copy(gridColumns = nextCols, gridRows = nextRows)
            }
            val wide = if (_state.value.isWideScreen) nextCols + 2 else nextCols
            app.workspaceStore.update { WorkspaceEngine.reflow(it, wide, nextRows) }
        }
    }

    fun toggleDock(appInfo: AppInfo) {
        if (!_state.value.profile.dock) return
        viewModelScope.launch { app.favoritesStore.toggleDock(appInfo.key) }
    }

    fun openWallpaperPicker() {
        actionHandler.startSafe(Intent(Intent.ACTION_SET_WALLPAPER))
    }

    fun pruneStaleItems() {
        viewModelScope.launch { pruneStaleItemsSuspend() }
    }

    private fun columns(): Int {
        val settings = _state.value.settings
        return if (_state.value.isWideScreen) settings.gridColumns + 2 else settings.gridColumns
    }

    private fun nextSlot(
        items: List<WorkspaceItem>,
        spanX: Int,
        spanY: Int,
    ): Triple<Int, Int, Int>? {
        val cols = columns()
        val rows = _state.value.settings.gridRows
        var page = 0
        while (page < 10) {
            val free = WorkspaceEngine.firstFree(items, page, cols, rows, spanX, spanY)
            if (free != null) return Triple(page, free.first, free.second)
            page++
        }
        return null
    }

    private suspend fun placeWidget(widget: PinnedWidget) {
        app.workspaceStore.update { items ->
            if (items.any { it.widgetId == widget.appWidgetId }) items
            else {
                val slot = nextSlot(items, 2, 2) ?: return@update items
                items + WorkspaceItem(
                    id = UUID.randomUUID().toString(),
                    type = WorkspaceType.WIDGET,
                    page = slot.first,
                    cellX = slot.second,
                    cellY = slot.third,
                    spanX = 2,
                    spanY = 2,
                    widgetId = widget.appWidgetId,
                    provider = widget.provider,
                )
            }
        }
    }

    fun toggleHidden(appInfo: AppInfo) {
        if (!_state.value.profile.vault) return
        viewModelScope.launch { app.favoritesStore.toggleHidden(appInfo.packageName) }
        closeOverlays()
    }

    fun uninstall(appInfo: AppInfo) {
        actionHandler.startSafe(Intent(Intent.ACTION_DELETE, Uri.parse("package:${appInfo.packageName}")))
    }

    fun openAppInfo(appInfo: AppInfo) {
        actionHandler.startSafe(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${appInfo.packageName}")))
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { app.settingsStore.update(transform) }
    }

    fun onSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(120)
            val profile = _state.value.profile
            val currentApps = _state.value.apps
            val searchContacts = _state.value.settings.searchContacts
            val actions = searchActions(profile)
            val shortcuts = cachedShortcuts
            val results = withContext(Dispatchers.IO) {
                app.searchEngine.query(
                    raw = query,
                    apps = currentApps,
                    shortcuts = shortcuts,
                    includeContacts = profile.contactsSearch && searchContacts,
                    allowedActions = actions,
                    onAction = { id ->
                        viewModelScope.launch(Dispatchers.Main.immediate) { handleSearchAction(id) }
                    },
                    onShortcut = { shortcut ->
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            val host = currentApps.firstOrNull { it.packageName == shortcut.packageName }
                            if (host != null) launchShortcut(host, shortcut)
                            else app.appsStore.launchShortcut(
                                shortcut.packageName,
                                shortcut.id,
                                android.os.Process.myUserHandle(),
                            )
                        }
                    },
                )
            }
            _state.update { it.copy(searchResults = results) }
        }
    }

    fun handleSearchAction(id: String) {
        when (id) {
            "settings" -> open(Overlay.Settings)
            "drawer" -> open(Overlay.Drawer)
            "search" -> open(Overlay.Search)
            "control" -> if (_state.value.profile.controlCenter) open(Overlay.ControlCenter)
            "wallpaper" -> applyBingWallpaper()
            "widget" -> openWidgetPicker()
            "vault" -> openVault()
            "backup" -> open(Overlay.Settings)
        }
    }

    fun applyBingWallpaper() {
        if (!_state.value.profile.bingWallpaper) return
        viewModelScope.launch {
            val result = app.wallpaperFetcher.applyBingDaily()
            val context = getApplication<Application>()
            Toast.makeText(
                context,
                if (result.isSuccess) "Bing günlük duvar kağıdı uygulandı" else "Duvar kağıdı alınamadı",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun openDefaultHomePicker() {
        LauncherComponents.enableHomeRole(getApplication())
        actionHandler.openDefaultHomePicker()
    }

    fun openDreamSettings() = actionHandler.openDreamSettings()

    fun openUsageAccess() = actionHandler.openUsageAccess()

    fun openNotificationAccess() = actionHandler.openNotificationAccess()

    fun openSystemSettings() = actionHandler.openSystemSettings()

    fun openWifiSettings() = actionHandler.openWifiSettings()

    fun openBluetoothSettings() = actionHandler.openBluetoothSettings()

    fun expandNotifications() = actionHandler.expandNotifications()

    fun toggleFlashlight() {
        if (!_state.value.profile.controlCenter) return
        actionHandler.toggleFlashlight(_state.value.flashlightOn) { next ->
            _state.update { it.copy(flashlightOn = next) }
        }
    }

    fun checkForUpdate(force: Boolean = false) {
        val snapshot = _state.value
        if (!snapshot.settings.onboardingDone || !snapshot.profile.isChosen) return
        if (snapshot.overlay == Overlay.Onboarding) return
        if (!force && updateChecked) return
        updateChecked = true
        viewModelScope.launch {
            when (val result = updateManager.check(snapshot.settings, force)) {
                is UpdateCheckResult.NewUpdate -> {
                    _state.update {
                        it.copy(
                            pendingUpdate = result.offer,
                            overlay = if (it.overlay == Overlay.None) Overlay.Update else it.overlay,
                        )
                    }
                }
                UpdateCheckResult.NoUrl -> {
                    if (force) toast("Güncelleme adresi yok. Ayarlar’dan latest.json adresini girin veya yeni APK’yı üzerine kurun.")
                }
                UpdateCheckResult.Failed -> {
                    if (force) toast("Güncelleme denetimi başarısız")
                }
                UpdateCheckResult.UpToDate -> {
                    if (force) toast("Uygulama güncel (${BuildConfig.VERSION_NAME})")
                }
                UpdateCheckResult.Postponed -> Unit
            }
        }
    }

    fun postponeUpdate() {
        val offer = _state.value.pendingUpdate ?: return
        updateSettings {
            it.copy(
                postponedVersionCode = offer.versionCode,
                postponedUntil = System.currentTimeMillis() + 24L * 60L * 60L * 1000L,
            )
        }
        _state.update { it.copy(overlay = Overlay.None, updateStatus = "") }
    }

    fun applyPendingUpdate() {
        val offer = _state.value.pendingUpdate ?: return
        if (offer.apkUrl.isBlank()) {
            toast("Yeni APK’yı APK klasöründen kurun. Aynı imzayla üzerine yazınca ayarlar korunur.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(updateStatus = "İndiriliyor…") }
            val result = updateManager.downloadAndPrepareInstall(offer) { status ->
                _state.update { it.copy(updateStatus = status) }
            }
            result.onSuccess { intent ->
                _state.update { it.copy(updateStatus = "Kurulum açılıyor") }
                actionHandler.startSafe(intent)
            }.onFailure { error ->
                _state.update { it.copy(updateStatus = "") }
                toast(error.message ?: "Güncelleme indirilemedi")
            }
        }
    }

    fun moveFavorite(appInfo: AppInfo, delta: Int) {
        if (!_state.value.profile.isChosen) return
        viewModelScope.launch { app.favoritesStore.moveHome(appInfo.key, delta) }
    }

    fun lastCrashLog(): String = com.makay.launcher.data.CrashLogger.lastCrash(getApplication())

    fun shareCrashLog() {
        val intent = com.makay.launcher.data.CrashLogger.shareIntent(getApplication())
        if (intent == null) {
            toast("Kayıtlı çökme yok")
            return
        }
        actionHandler.startSafe(Intent.createChooser(intent, "Çökme kaydını paylaş"))
    }

    fun clearCrashLog() {
        com.makay.launcher.data.CrashLogger.clear(getApplication())
        toast("Çökme kaydı silindi")
    }

    fun hasUsageAccess(): Boolean = com.makay.launcher.data.SystemUsage.hasAccess(getApplication())

    fun showPendingUpdate() {
        if (_state.value.pendingUpdate != null) open(Overlay.Update)
    }

    private fun toast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
        }
    }

    fun setWideScreen(wide: Boolean) {
        _state.update { it.copy(isWideScreen = wide) }
    }

    fun refreshFormFactor() {
        val factor = FormFactorDetector.detect(getApplication())
        _state.update {
            it.copy(
                formFactor = factor,
                isWideScreen = factor != FormFactor.PHONE,
            )
        }
    }

    fun rebindWidget(provider: String) {
        if (provider.isBlank()) return
        viewModelScope.launch {
            val restored = app.widgetController.restoreProvider(provider)
            if (restored != null) {
                app.widgetController.persist(restored)
                toast("Widget yeniden bağlandı")
            } else {
                toast("Widget bağlanamadı. İzin penceresini onaylayın veya widget’ı yeniden ekleyin.")
            }
        }
    }

    fun addWidget(offer: WidgetOffer) {
        if (!_state.value.profile.widgets) return
        app.widgetController.requestAdd(offer)
        closeOverlays()
    }

    fun removeWidget(widget: PinnedWidget) {
        if (!_state.value.profile.widgets) return
        viewModelScope.launch { app.widgetController.remove(widget) }
    }

    fun openWidgetPicker() {
        if (!_state.value.profile.widgets) return
        viewModelScope.launch(Dispatchers.IO) {
            val offers = app.widgetController.offers()
            _state.update { it.copy(widgetOffers = offers, overlay = Overlay.WidgetPicker) }
        }
    }

    fun applyIconPack(packageName: String) {
        app.iconCache.clear()
        updateSettings { it.copy(iconPackPackage = packageName) }
    }

    fun launchInput(source: InputSource) {
        if (!_state.value.profile.tvInputs) return
        app.tvInputStore.launch(source)
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { app.backupRepository.exportTo(uri) }
            launch(Dispatchers.Main) {
                Toast.makeText(
                    getApplication(),
                    if (result.isSuccess) "Yedek dışa aktarıldı" else "Yedek alınamadı",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { app.backupRepository.importFrom(uri) }
            launch(Dispatchers.Main) {
                Toast.makeText(
                    getApplication(),
                    if (result.isSuccess) "Yedek geri yüklendi" else "Geri yükleme başarısız",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun reloadCatalogs() {
        val profile = _state.value.profile
        val packs = if (profile.iconPacks) app.iconPackEngine.installedPacks() else emptyList()
        val offers = if (profile.widgets) {
            runCatching { app.widgetController.offers() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val inputs = if (profile.tvInputs) app.tvInputStore.sources() else emptyList()
        _state.update {
            it.copy(iconPacks = packs, widgetOffers = offers, inputSources = inputs)
        }
    }

    private fun resolveEdition(settings: LauncherSettings): AppEdition {
        if (settings.edition != AppEdition.UNSET) return settings.edition
        if (!settings.onboardingDone) return AppEdition.UNSET
        val inferred = FormFactorDetector.suggestedEdition(getApplication())
        viewModelScope.launch { app.settingsStore.update { it.copy(edition = inferred) } }
        return inferred
    }

    private fun syncRuntime(profile: EditionProfile) {
        if (profile.headphones && audioCallback == null) {
            audioCallback = app.glanceIntel.registerAudio { refreshGlance() }
        } else if (!profile.headphones) {
            audioCallback?.let { app.glanceIntel.unregisterAudio(it) }
            audioCallback = null
        }
        if (!profile.widgets) {
            _state.update { it.copy(widgets = emptyList(), widgetOffers = emptyList()) }
        }
        if (!profile.tvInputs) {
            _state.update { it.copy(inputSources = emptyList()) }
        }
        if (!profile.notificationBadges) {
            _state.update { it.copy(badges = emptyMap()) }
        }
    }

    private fun overlayAllowed(overlay: Overlay): Boolean {
        val profile = _state.value.profile
        return when (overlay) {
            Overlay.None, Overlay.Settings, Overlay.Onboarding, Overlay.Update -> true
            Overlay.Drawer, Overlay.Search -> profile.isChosen
            Overlay.ControlCenter -> profile.controlCenter
            Overlay.WidgetPicker -> profile.widgets
            Overlay.Vault -> profile.vault
            Overlay.Folder -> profile.folders
        }
    }

    private fun searchActions(profile: EditionProfile): Set<String> = buildSet {
        if (!profile.search) return@buildSet
        add("settings")
        add("drawer")
        add("search")
        add("backup")
        if (profile.controlCenter) add("control")
        if (profile.bingWallpaper) add("wallpaper")
        if (profile.widgets) add("widget")
        if (profile.vault) add("vault")
    }

    fun webSearch(query: String) {
        actionHandler.startSafe(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query))
    }

    fun isNotificationAccessGranted(): Boolean {
        val context = getApplication<Application>()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        val cn = ComponentName(context, BadgeNotificationListener::class.java).flattenToString()
        return enabled.contains(context.packageName) && enabled.contains(cn.substringAfter("/"))
    }

    private fun loadShortcuts(appInfo: AppInfo): List<AppShortcut> {
        val context = getApplication<Application>()
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(appInfo.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return runCatching {
            launcherApps.getShortcuts(query, appInfo.user).orEmpty().map {
                AppShortcut(it.id, it.shortLabel?.toString() ?: it.id, appInfo.packageName)
            }
        }.getOrDefault(emptyList())
    }

    private fun defaultDock(apps: List<AppInfo>): List<AppInfo> {
        val preferred = listOf("dialer", "phone", "mms", "messaging", "chrome", "camera", "vending")
        val picked = preferred.mapNotNull { key ->
            apps.firstOrNull { it.packageName.lowercase().contains(key) }
        }.distinctBy { it.key }
        return (picked + apps.take(5)).distinctBy { it.key }.take(5)
    }

    private suspend fun prunePackage(packageName: String) {
        val prefix = "$packageName/"
        app.favoritesStore.prunePackage(packageName)
        app.workspaceStore.update { items ->
            items.mapNotNull { item ->
                when (item.type) {
                    WorkspaceType.APP -> item.takeUnless { it.appKey.startsWith(prefix) }
                    WorkspaceType.FOLDER -> {
                        val members = item.members.filterNot { it.startsWith(prefix) }
                        item.copy(members = members).takeIf { members.isNotEmpty() }
                    }
                    WorkspaceType.WIDGET -> item.takeUnless { it.provider.startsWith(prefix) }
                }
            }
        }
        app.widgetController.snapshotPinned()
            .filter { it.provider.startsWith(prefix) }
            .forEach { app.widgetController.remove(it) }
    }

    private suspend fun pruneStaleItemsSuspend() {
        app.widgetController.pruneOrphans()
        app.workspaceStore.update { items ->
            items.filter { item ->
                item.type != WorkspaceType.WIDGET ||
                    (item.widgetId > 0 && app.widgetController.infoOrNull(item.widgetId) != null)
            }
        }
        if (_state.value.settings.digitalDetox) {
            _state.update { it.copy(badges = emptyMap()) }
        }
    }

    override fun onCleared() {
        monitor.stop()
        audioCallback?.let { app.glanceIntel.unregisterAudio(it) }
        super.onCleared()
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(application) as T
                }
            }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component1() = first
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component2() = second
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component3() = third
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component4() = fourth
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component5() = fifth
