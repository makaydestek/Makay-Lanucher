package com.makay.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makay.launcher.domain.AppEdition
import com.makay.launcher.domain.GestureAction
import com.makay.launcher.domain.HomeStyle
import com.makay.launcher.domain.IconShape
import com.makay.launcher.domain.LauncherSettings
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
    }

    suspend fun snapshot(): LauncherSettings = settings.first()

    suspend fun restore(next: LauncherSettings) {
        update { next }
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        raw?.let { runCatching { java.lang.Enum.valueOf(T::class.java, it) }.getOrNull() } ?: fallback
}
