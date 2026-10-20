package com.makay.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makay.launcher.domain.AppEdition
import com.makay.launcher.domain.GestureAction
import com.makay.launcher.domain.HomeStyle
import com.makay.launcher.domain.IconShape
import com.makay.launcher.domain.LauncherSettings
import com.makay.launcher.domain.TvCardSize
import com.makay.launcher.domain.TvFocusEffect
import com.makay.launcher.domain.TvIdleAction
import com.makay.launcher.domain.TvWallpaperKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val style = stringPreferencesKey("home_style")
    private val swipeUp = stringPreferencesKey("swipe_up")
    private val swipeDown = stringPreferencesKey("swipe_down")
    private val doubleTap = stringPreferencesKey("double_tap")
    private val glance = booleanPreferencesKey("glance")
    private val dock = booleanPreferencesKey("dock")
    private val rail = booleanPreferencesKey("rail")
    private val textOnly = booleanPreferencesKey("text_only")
    private val columns = intPreferencesKey("columns")
    private val contacts = booleanPreferencesKey("contacts")
    private val categories = booleanPreferencesKey("categories")
    private val detox = booleanPreferencesKey("detox")
    private val iconPack = stringPreferencesKey("icon_pack")
    private val dynamic = booleanPreferencesKey("dynamic_colors")
    private val onboard = booleanPreferencesKey("onboarding")
    private val shape = stringPreferencesKey("icon_shape")
    private val rows = intPreferencesKey("rows")
    private val edition = stringPreferencesKey("app_edition")
    private val updateUrl = stringPreferencesKey("update_url")
    private val postponeCode = intPreferencesKey("postpone_code")
    private val postponeUntil = longPreferencesKey("postpone_until")

    private val tvCardSize = stringPreferencesKey("tv_card_size")
    private val tvCardCorner = intPreferencesKey("tv_card_corner")
    private val tvCardOpacity = floatPreferencesKey("tv_card_opacity")
    private val tvFocusEffect = stringPreferencesKey("tv_focus_effect")
    private val tvHideAbove = booleanPreferencesKey("tv_hide_above")
    private val tvChannels = booleanPreferencesKey("tv_channels")
    private val tvWpKind = stringPreferencesKey("tv_wp_kind")
    private val tvWpUri = stringPreferencesKey("tv_wp_uri")
    private val tvWpBright = floatPreferencesKey("tv_wp_bright")
    private val tvWpBlur = intPreferencesKey("tv_wp_blur")
    private val tvWpDim = floatPreferencesKey("tv_wp_dim")
    private val tvAutostart = stringPreferencesKey("tv_autostart")
    private val tvIdleAction = stringPreferencesKey("tv_idle_action")
    private val tvIdleMin = intPreferencesKey("tv_idle_min")
    private val tvParental = booleanPreferencesKey("tv_parental")
    private val tvPin = stringPreferencesKey("tv_pin_hash")
    private val tvSchedule = booleanPreferencesKey("tv_schedule")
    private val tvStart = intPreferencesKey("tv_start_min")
    private val tvEnd = intPreferencesKey("tv_end_min")
    private val tvBlockedPkg = stringPreferencesKey("tv_blocked_pkg")
    private val tvBlockedIn = stringPreferencesKey("tv_blocked_in")

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data.map(::read)

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        context.settingsDataStore.edit { prefs ->
            write(prefs, transform(read(prefs)))
        }
    }

    private fun read(prefs: Preferences) = LauncherSettings(
        homeStyle = enumValue(prefs[style], HomeStyle.GRID),
        swipeUp = enumValue(prefs[swipeUp], GestureAction.Drawer),
        swipeDown = enumValue(prefs[swipeDown], GestureAction.Search),
        doubleTap = enumValue(prefs[doubleTap], GestureAction.ControlCenter),
        showAtAGlance = prefs[glance] ?: true,
        showDock = prefs[dock] ?: true,
        showAlphabetRail = prefs[rail] ?: true,
        textOnly = prefs[textOnly] ?: false,
        gridColumns = prefs[columns] ?: 4,
        searchContacts = prefs[contacts] ?: false,
        autoCategories = prefs[categories] ?: true,
        digitalDetox = prefs[detox] ?: false,
        iconPackPackage = prefs[iconPack].orEmpty(),
        dynamicColors = prefs[dynamic] ?: true,
        onboardingDone = prefs[onboard] ?: false,
        iconShape = enumValue(prefs[shape], IconShape.SQUIRCLE),
        gridRows = prefs[rows] ?: 5,
        edition = enumValue(prefs[edition], AppEdition.UNSET),
        updateUrl = prefs[updateUrl].orEmpty(),
        postponedVersionCode = prefs[postponeCode] ?: 0,
        postponedUntil = prefs[postponeUntil] ?: 0L,
        tvCardSize = enumValue(prefs[tvCardSize], TvCardSize.MEDIUM),
        tvCardCorner = prefs[tvCardCorner] ?: 18,
        tvCardOpacity = prefs[tvCardOpacity] ?: 0.85f,
        tvFocusEffect = enumValue(prefs[tvFocusEffect], TvFocusEffect.SCALE),
        tvHideRowsAboveFocus = prefs[tvHideAbove] ?: false,
        tvChannelsEnabled = prefs[tvChannels] ?: true,
        tvWallpaperKind = enumValue(prefs[tvWpKind], TvWallpaperKind.NONE),
        tvWallpaperUri = prefs[tvWpUri].orEmpty(),
        tvWallpaperBrightness = prefs[tvWpBright] ?: 1f,
        tvWallpaperBlur = prefs[tvWpBlur] ?: 0,
        tvWallpaperDim = prefs[tvWpDim] ?: 0.35f,
        tvAutostartKey = prefs[tvAutostart].orEmpty(),
        tvIdleAction = enumValue(prefs[tvIdleAction], TvIdleAction.NONE),
        tvIdleMinutes = prefs[tvIdleMin] ?: 5,
        tvParentalEnabled = prefs[tvParental] ?: false,
        tvParentalPinHash = prefs[tvPin].orEmpty(),
        tvParentalSchedule = prefs[tvSchedule] ?: false,
        tvParentalStartMin = prefs[tvStart] ?: (21 * 60),
        tvParentalEndMin = prefs[tvEnd] ?: (7 * 60),
        tvBlockedPackages = prefs[tvBlockedPkg].orEmpty(),
        tvBlockedInputs = prefs[tvBlockedIn].orEmpty(),
    )

    private fun write(prefs: MutablePreferences, next: LauncherSettings) {
        prefs[style] = next.homeStyle.name
        prefs[swipeUp] = next.swipeUp.name
        prefs[swipeDown] = next.swipeDown.name
        prefs[doubleTap] = next.doubleTap.name
        prefs[glance] = next.showAtAGlance
        prefs[dock] = next.showDock
        prefs[rail] = next.showAlphabetRail
        prefs[textOnly] = next.textOnly
        prefs[columns] = next.gridColumns
        prefs[contacts] = next.searchContacts
        prefs[categories] = next.autoCategories
        prefs[detox] = next.digitalDetox
        prefs[iconPack] = next.iconPackPackage
        prefs[dynamic] = next.dynamicColors
        prefs[onboard] = next.onboardingDone
        prefs[shape] = next.iconShape.name
        prefs[rows] = next.gridRows
        prefs[edition] = next.edition.name
        prefs[updateUrl] = next.updateUrl
        prefs[postponeCode] = next.postponedVersionCode
        prefs[postponeUntil] = next.postponedUntil
        prefs[tvCardSize] = next.tvCardSize.name
        prefs[tvCardCorner] = next.tvCardCorner
        prefs[tvCardOpacity] = next.tvCardOpacity
        prefs[tvFocusEffect] = next.tvFocusEffect.name
        prefs[tvHideAbove] = next.tvHideRowsAboveFocus
        prefs[tvChannels] = next.tvChannelsEnabled
        prefs[tvWpKind] = next.tvWallpaperKind.name
        prefs[tvWpUri] = next.tvWallpaperUri
        prefs[tvWpBright] = next.tvWallpaperBrightness
        prefs[tvWpBlur] = next.tvWallpaperBlur
        prefs[tvWpDim] = next.tvWallpaperDim
        prefs[tvAutostart] = next.tvAutostartKey
        prefs[tvIdleAction] = next.tvIdleAction.name
        prefs[tvIdleMin] = next.tvIdleMinutes
        prefs[tvParental] = next.tvParentalEnabled
        prefs[tvPin] = next.tvParentalPinHash
        prefs[tvSchedule] = next.tvParentalSchedule
        prefs[tvStart] = next.tvParentalStartMin
        prefs[tvEnd] = next.tvParentalEndMin
        prefs[tvBlockedPkg] = next.tvBlockedPackages
        prefs[tvBlockedIn] = next.tvBlockedInputs
    }

    suspend fun snapshot(): LauncherSettings = settings.first()

    suspend fun restore(next: LauncherSettings) {
        update { next }
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        raw?.let { runCatching { java.lang.Enum.valueOf(T::class.java, it) }.getOrNull() } ?: fallback
}
