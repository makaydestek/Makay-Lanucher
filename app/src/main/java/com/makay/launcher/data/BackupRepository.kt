package com.makay.launcher.data

import android.content.Context
import android.net.Uri
import com.makay.launcher.domain.WorkspaceItem
import com.makay.launcher.domain.WorkspaceType
import com.makay.launcher.domain.forEdition
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val favoritesStore: FavoritesStore,
    private val widgetController: WidgetController,
    private val workspaceStore: WorkspaceStore,
    private val tvLayoutStore: TvLayoutStore,
    private val tvOverridesStore: TvAppOverridesStore? = null,
) {
    suspend fun exportJson(): String {
        val settings = settingsStore.snapshot()
        val favorites = favoritesStore.snapshot()
        val widgets = widgetController.snapshotPinned()
        val json = buildBackupJson(settings, favorites, widgets)
        return json.toString(2)
    }

    suspend fun exportTo(uri: Uri) {
        val settings = settingsStore.snapshot()
        val favorites = favoritesStore.snapshot()
        val widgets = widgetController.snapshotPinned()
        val json = buildBackupJson(settings, favorites, widgets)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toString(2).toByteArray(Charsets.UTF_8))
        } ?: error("Yedek yazılamadı")
    }

    suspend fun importJson(raw: String) {
        applyBackup(JSONObject(raw))
    }

    private suspend fun buildBackupJson(
        settings: com.makay.launcher.domain.LauncherSettings,
        favorites: FavoriteSnapshot,
        widgets: List<com.makay.launcher.domain.PinnedWidget>,
    ): JSONObject {
        val json = JSONObject()
            .put("version", 4)
            .put(
                "settings",
                JSONObject()
                    .put("homeStyle", settings.homeStyle.name)
                    .put("swipeUp", settings.swipeUp.name)
                    .put("swipeDown", settings.swipeDown.name)
                    .put("doubleTap", settings.doubleTap.name)
                    .put("showAtAGlance", settings.showAtAGlance)
                    .put("showDock", settings.showDock)
                    .put("showAlphabetRail", settings.showAlphabetRail)
                    .put("textOnly", settings.textOnly)
                    .put("gridColumns", settings.gridColumns)
                    .put("searchContacts", settings.searchContacts)
                    .put("autoCategories", settings.autoCategories)
                    .put("digitalDetox", settings.digitalDetox)
                    .put("iconPackPackage", settings.iconPackPackage)
                    .put("dynamicColors", settings.dynamicColors)
                    .put("iconShape", settings.iconShape.name)
                    .put("gridRows", settings.gridRows)
                    .put("edition", settings.edition.name)
                    .put("tvCardSize", settings.tvCardSize.name)
                    .put("tvCardCorner", settings.tvCardCorner)
                    .put("tvCardOpacity", settings.tvCardOpacity.toDouble())
                    .put("tvFocusEffect", settings.tvFocusEffect.name)
                    .put("tvHideRowsAboveFocus", settings.tvHideRowsAboveFocus)
                    .put("tvChannelsEnabled", settings.tvChannelsEnabled)
                    .put("tvWallpaperKind", settings.tvWallpaperKind.name)
                    .put("tvWallpaperUri", settings.tvWallpaperUri)
                    .put("tvWallpaperBrightness", settings.tvWallpaperBrightness.toDouble())
                    .put("tvWallpaperBlur", settings.tvWallpaperBlur)
                    .put("tvWallpaperDim", settings.tvWallpaperDim.toDouble())
                    .put("tvAutostartKey", settings.tvAutostartKey)
                    .put("tvIdleAction", settings.tvIdleAction.name)
                    .put("tvIdleMinutes", settings.tvIdleMinutes)
                    .put("tvParentalEnabled", settings.tvParentalEnabled)
                    .put("tvParentalPinHash", settings.tvParentalPinHash)
                    .put("tvParentalSchedule", settings.tvParentalSchedule)
                    .put("tvParentalStartMin", settings.tvParentalStartMin)
                    .put("tvParentalEndMin", settings.tvParentalEndMin)
                    .put("tvBlockedPackages", settings.tvBlockedPackages)
                    .put("tvBlockedInputs", settings.tvBlockedInputs)
                    .put("tvCardSpacing", settings.tvCardSpacing)
                    .put("tvCardIconMode", settings.tvCardIconMode.name)
                    .put("tvRememberFocus", settings.tvRememberFocus)
                    .put("tvBurnInFade", settings.tvBurnInFade)
                    .put("tvShowClock", settings.tvShowClock)
                    .put("tvHideSettingsChip", settings.tvHideSettingsChip)
                    .put("tvVideoPreviewOnFocus", settings.tvVideoPreviewOnFocus)
                    .put("tvWatchProgressBar", settings.tvWatchProgressBar)
                    .put("tvVideoZoom", settings.tvVideoZoom.toDouble())
                    .put("tvWallpaperContrast", settings.tvWallpaperContrast.toDouble())
                    .put("tvWallpaperSaturation", settings.tvWallpaperSaturation.toDouble())
                    .put("tvWallpaperHue", settings.tvWallpaperHue.toDouble())
                    .put("tvBootToScreensaver", settings.tvBootToScreensaver)
                    .put("tvFamilySlots", settings.tvFamilySlots)
                    .put("tvGuestLock", settings.tvGuestLock)
                    .put("tvAccessibilityAssist", settings.tvAccessibilityAssist)
                    .put("tvLocaleTag", settings.tvLocaleTag)
                    .put("tvAutoBackup", settings.tvAutoBackup),
            )
            .put("tvRows", run {
                val encoded = tvLayoutStore.encode(tvLayoutStore.snapshot())
                if (encoded.isBlank()) JSONArray() else JSONArray(encoded)
            })
            .put("tvOverrides", run {
                val encoded = tvOverridesStore?.encode(tvOverridesStore.snapshot()).orEmpty()
                if (encoded.isBlank()) JSONArray() else JSONArray(encoded)
            })
            .put("home", JSONArray(favorites.home))
            .put("dock", JSONArray(favorites.dock))
            .put("hidden", JSONArray(favorites.hidden.toList()))
            .put("locked", JSONArray(favorites.locked.toList()))
            .put(
                "widgets",
                JSONArray().also { array ->
                    widgets.forEach { widget ->
                        array.put(
                            JSONObject()
                                .put("id", widget.appWidgetId)
                                .put("provider", widget.provider),
                        )
                    }
                },
            )
        json.put("workspace", JSONArray(workspaceStore.snapshot().map { item ->
            JSONObject()
                .put("id", item.id)
                .put("type", item.type.name)
                .put("page", item.page)
                .put("cellX", item.cellX)
                .put("cellY", item.cellY)
                .put("spanX", item.spanX)
                .put("spanY", item.spanY)
                .put("appKey", item.appKey)
                .put("title", item.title)
                .put("members", JSONArray(item.members))
                    .put("widgetId", item.widgetId)
                    .put("provider", item.provider)
                    .put("color", item.color)
                    .put("shortcutId", item.shortcutId)
        }))
        return json
    }

    suspend fun importFrom(uri: Uri) {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Yedek okunamadı")
        applyBackup(JSONObject(raw))
    }

    private suspend fun applyBackup(json: JSONObject) {
        val settingsJson = json.optJSONObject("settings")
        if (settingsJson != null) {
            val current = settingsStore.snapshot()
            settingsStore.restore(
                current.copy(
                    homeStyle = enumOr(settingsJson.optString("homeStyle"), current.homeStyle),
                    swipeUp = enumOr(settingsJson.optString("swipeUp"), current.swipeUp),
                    swipeDown = enumOr(settingsJson.optString("swipeDown"), current.swipeDown),
                    doubleTap = enumOr(settingsJson.optString("doubleTap"), current.doubleTap),
                    showAtAGlance = settingsJson.optBoolean("showAtAGlance", current.showAtAGlance),
                    showDock = settingsJson.optBoolean("showDock", current.showDock),
                    showAlphabetRail = settingsJson.optBoolean("showAlphabetRail", current.showAlphabetRail),
                    textOnly = settingsJson.optBoolean("textOnly", current.textOnly),
                    gridColumns = settingsJson.optInt("gridColumns", current.gridColumns),
                    searchContacts = settingsJson.optBoolean("searchContacts", current.searchContacts),
                    autoCategories = settingsJson.optBoolean("autoCategories", current.autoCategories),
                    digitalDetox = settingsJson.optBoolean("digitalDetox", current.digitalDetox),
                    iconPackPackage = settingsJson.optString("iconPackPackage", current.iconPackPackage),
                    dynamicColors = settingsJson.optBoolean("dynamicColors", current.dynamicColors),
                    iconShape = enumOr(settingsJson.optString("iconShape"), current.iconShape),
                    gridRows = settingsJson.optInt("gridRows", current.gridRows),
                    edition = enumOr(settingsJson.optString("edition"), current.edition),
                    tvCardSize = enumOr(settingsJson.optString("tvCardSize"), current.tvCardSize),
                    tvCardCorner = settingsJson.optInt("tvCardCorner", current.tvCardCorner),
                    tvCardOpacity = settingsJson.optDouble("tvCardOpacity", current.tvCardOpacity.toDouble()).toFloat(),
                    tvFocusEffect = enumOr(settingsJson.optString("tvFocusEffect"), current.tvFocusEffect),
                    tvHideRowsAboveFocus = settingsJson.optBoolean("tvHideRowsAboveFocus", current.tvHideRowsAboveFocus),
                    tvChannelsEnabled = settingsJson.optBoolean("tvChannelsEnabled", current.tvChannelsEnabled),
                    tvWallpaperKind = enumOr(settingsJson.optString("tvWallpaperKind"), current.tvWallpaperKind),
                    tvWallpaperUri = settingsJson.optString("tvWallpaperUri", current.tvWallpaperUri),
                    tvWallpaperBrightness = settingsJson.optDouble("tvWallpaperBrightness", current.tvWallpaperBrightness.toDouble()).toFloat(),
                    tvWallpaperBlur = settingsJson.optInt("tvWallpaperBlur", current.tvWallpaperBlur),
                    tvWallpaperDim = settingsJson.optDouble("tvWallpaperDim", current.tvWallpaperDim.toDouble()).toFloat(),
                    tvAutostartKey = settingsJson.optString("tvAutostartKey", current.tvAutostartKey),
                    tvIdleAction = enumOr(settingsJson.optString("tvIdleAction"), current.tvIdleAction),
                    tvIdleMinutes = settingsJson.optInt("tvIdleMinutes", current.tvIdleMinutes),
                    tvParentalEnabled = settingsJson.optBoolean("tvParentalEnabled", current.tvParentalEnabled),
                    tvParentalPinHash = settingsJson.optString("tvParentalPinHash", current.tvParentalPinHash),
                    tvParentalSchedule = settingsJson.optBoolean("tvParentalSchedule", current.tvParentalSchedule),
                    tvParentalStartMin = settingsJson.optInt("tvParentalStartMin", current.tvParentalStartMin),
                    tvParentalEndMin = settingsJson.optInt("tvParentalEndMin", current.tvParentalEndMin),
                    tvBlockedPackages = settingsJson.optString("tvBlockedPackages", current.tvBlockedPackages),
                    tvBlockedInputs = settingsJson.optString("tvBlockedInputs", current.tvBlockedInputs),
                    tvCardSpacing = settingsJson.optInt("tvCardSpacing", current.tvCardSpacing),
                    tvCardIconMode = enumOr(settingsJson.optString("tvCardIconMode"), current.tvCardIconMode),
                    tvRememberFocus = settingsJson.optBoolean("tvRememberFocus", current.tvRememberFocus),
                    tvBurnInFade = settingsJson.optBoolean("tvBurnInFade", current.tvBurnInFade),
                    tvShowClock = settingsJson.optBoolean("tvShowClock", current.tvShowClock),
                    tvHideSettingsChip = settingsJson.optBoolean("tvHideSettingsChip", current.tvHideSettingsChip),
                    tvVideoPreviewOnFocus = settingsJson.optBoolean("tvVideoPreviewOnFocus", current.tvVideoPreviewOnFocus),
                    tvWatchProgressBar = settingsJson.optBoolean("tvWatchProgressBar", current.tvWatchProgressBar),
                    tvVideoZoom = settingsJson.optDouble("tvVideoZoom", current.tvVideoZoom.toDouble()).toFloat(),
                    tvWallpaperContrast = settingsJson.optDouble("tvWallpaperContrast", current.tvWallpaperContrast.toDouble()).toFloat(),
                    tvWallpaperSaturation = settingsJson.optDouble("tvWallpaperSaturation", current.tvWallpaperSaturation.toDouble()).toFloat(),
                    tvWallpaperHue = settingsJson.optDouble("tvWallpaperHue", current.tvWallpaperHue.toDouble()).toFloat(),
                    tvBootToScreensaver = settingsJson.optBoolean("tvBootToScreensaver", current.tvBootToScreensaver),
                    tvFamilySlots = settingsJson.optString("tvFamilySlots", current.tvFamilySlots),
                    tvGuestLock = settingsJson.optBoolean("tvGuestLock", current.tvGuestLock),
                    tvAccessibilityAssist = settingsJson.optBoolean("tvAccessibilityAssist", current.tvAccessibilityAssist),
                    tvLocaleTag = settingsJson.optString("tvLocaleTag", current.tvLocaleTag),
                    tvAutoBackup = settingsJson.optBoolean("tvAutoBackup", current.tvAutoBackup),
                ).let { restored ->
                    if (restored.edition != com.makay.launcher.domain.AppEdition.UNSET) {
                        restored.forEdition(restored.edition)
                    } else restored
                },
            )
        }
        json.optJSONArray("tvRows")?.let { array ->
            tvLayoutStore.replace(tvLayoutStore.parse(array.toString()))
        }
        json.optJSONArray("tvOverrides")?.let { array ->
            tvOverridesStore?.let { store ->
                store.parse(array.toString()).values.forEach { store.upsert(it) }
            }
        }
        widgetController.snapshotPinned().forEach { widgetController.remove(it) }
        val rebound = mutableMapOf<String, Int>()
        json.optJSONArray("workspace")?.let { array ->
            val items = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val members = obj.optJSONArray("members")
                    val type = enumOr(obj.optString("type"), WorkspaceType.APP)
                    val provider = obj.optString("provider")
                    val widgetId = if (type == WorkspaceType.WIDGET) {
                        restoreWidget(provider, rebound) ?: continue
                    } else {
                        -1
                    }
                    add(
                        WorkspaceItem(
                            id = obj.optString("id"),
                            type = type,
                            page = obj.optInt("page"),
                            cellX = obj.optInt("cellX"),
                            cellY = obj.optInt("cellY"),
                            spanX = obj.optInt("spanX", 1),
                            spanY = obj.optInt("spanY", 1),
                            appKey = obj.optString("appKey"),
                            title = obj.optString("title"),
                            members = buildList {
                                if (members != null) for (m in 0 until members.length()) add(members.optString(m))
                            },
                            widgetId = widgetId,
                            provider = provider,
                            color = obj.optInt("color"),
                            shortcutId = obj.optString("shortcutId"),
                        ),
                    )
                }
            }
            workspaceStore.replace(items)
        }
        json.optJSONArray("widgets")?.let { array ->
            for (i in 0 until array.length()) {
                val provider = array.getJSONObject(i).optString("provider")
                if (provider.isNotBlank() && provider !in rebound) {
                    restoreWidget(provider, rebound)
                }
            }
        }
        favoritesStore.restore(
            FavoriteSnapshot(
                home = json.optJSONArray("home").toStringList(),
                dock = json.optJSONArray("dock").toStringList(),
                hidden = json.optJSONArray("hidden").toStringList().toSet(),
                locked = json.optJSONArray("locked").toStringList().toSet(),
            ),
        )
    }

    private suspend fun restoreWidget(provider: String, rebound: MutableMap<String, Int>): Int? {
        if (provider.isBlank()) return null
        rebound[provider]?.let { return it }
        val restored = widgetController.restoreProvider(provider) ?: return null
        widgetController.persist(restored)
        rebound[provider] = restored.appWidgetId
        return restored.appWidgetId
    }

    private inline fun <reified T : Enum<T>> enumOr(raw: String, fallback: T): T =
        runCatching { java.lang.Enum.valueOf(T::class.java, raw) }.getOrDefault(fallback)

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) add(optString(i))
        }
    }
}
