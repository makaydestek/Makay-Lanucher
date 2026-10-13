package com.makay.launcher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makay.launcher.domain.PinnedWidget
import com.makay.launcher.domain.WidgetOffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.widgetDataStore by preferencesDataStore("widgets")

class MakayAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId)

class WidgetController(context: Context) {
    private val appContext = context.applicationContext
    val host = MakayAppWidgetHost(appContext, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    private val store = WidgetStore(appContext)

    val widgets: Flow<List<PinnedWidget>> = store.widgets

    var bindHandler: ((Intent) -> Unit)? = null
    var configureHandler: ((Intent) -> Unit)? = null
    var onPinned: ((PinnedWidget) -> Unit)? = null

    private var pendingId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: AppWidgetProviderInfo? = null

    fun offers(): List<WidgetOffer> = manager.installedProviders.map { info ->
        WidgetOffer(
            providerFlattened = info.provider.flattenToString(),
            label = info.loadLabel(appContext.packageManager).orEmpty().ifBlank { info.provider.className },
            minWidth = info.minWidth,
            minHeight = info.minHeight,
        )
    }.sortedBy { it.label.lowercase() }

    fun requestAdd(offer: WidgetOffer) {
        val provider = android.content.ComponentName.unflattenFromString(offer.providerFlattened) ?: return
        val info = manager.installedProviders.firstOrNull { it.provider == provider } ?: return
        val id = host.allocateAppWidgetId()
        pendingId = id
        pendingProvider = info
        val allowed = runCatching { manager.bindAppWidgetIdIfAllowed(id, provider) }.getOrDefault(false)
        if (allowed) {
            continueAfterBind()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            bindHandler?.invoke(intent)
        }
    }

    fun onBindResult(success: Boolean) {
        if (!success) {
            releasePending()
            return
        }
        continueAfterBind()
    }

    fun onConfigureResult(success: Boolean) {
        val id = pendingId
        val info = pendingProvider
        clearPending()
        if (!success || id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) host.deleteAppWidgetId(id)
            return
        }
        onPinned?.invoke(PinnedWidget(id, info?.provider?.flattenToString().orEmpty()))
    }

    suspend fun remove(widget: PinnedWidget) {
        runCatching { host.deleteAppWidgetId(widget.appWidgetId) }
        store.remove(widget.appWidgetId)
    }

    suspend fun persist(widget: PinnedWidget) {
        store.add(widget)
    }

    suspend fun snapshotPinned() = store.snapshot()

    fun infoOrNull(appWidgetId: Int) = runCatching { manager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    fun restoreProvider(provider: String): PinnedWidget? {
        val component = android.content.ComponentName.unflattenFromString(provider) ?: return null
        if (manager.installedProviders.none { it.provider == component }) return null
        val id = host.allocateAppWidgetId()
        val allowed = runCatching { manager.bindAppWidgetIdIfAllowed(id, component) }.getOrDefault(false)
        if (!allowed) {
            pendingId = id
            pendingProvider = manager.installedProviders.firstOrNull { it.provider == component }
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, component)
            }
            bindHandler?.invoke(intent)
            return PinnedWidget(id, provider)
        }
        return PinnedWidget(id, provider)
    }

    fun updateWidgetSize(appWidgetId: Int, minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int) {
        val options = android.os.Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)
        }
        runCatching { manager.updateAppWidgetOptions(appWidgetId, options) }
    }

    suspend fun pruneOrphans() {
        snapshotPinned().forEach { widget ->
            if (infoOrNull(widget.appWidgetId) == null) remove(widget)
        }
    }

    private fun continueAfterBind() {
        val info = pendingProvider ?: manager.getAppWidgetInfo(pendingId)
        if (info?.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId)
            }
            configureHandler?.invoke(intent)
        } else {
            onConfigureResult(true)
        }
    }

    private fun releasePending() {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            host.deleteAppWidgetId(pendingId)
        }
        clearPending()
    }

    private fun clearPending() {
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = null
    }

    companion object {
        const val HOST_ID = 0x4D4B
    }
}

class WidgetStore(private val context: Context) {
    private val key = stringPreferencesKey("pinned")

    val widgets: Flow<List<PinnedWidget>> = context.widgetDataStore.data.map { prefs ->
        parse(prefs[key].orEmpty())
    }

    suspend fun add(widget: PinnedWidget) {
        context.widgetDataStore.edit { prefs ->
            val current = parse(prefs[key].orEmpty()).toMutableList()
            current.removeAll { it.appWidgetId == widget.appWidgetId }
            current.add(widget)
            prefs[key] = encode(current)
        }
    }

    suspend fun remove(appWidgetId: Int) {
        context.widgetDataStore.edit { prefs ->
            prefs[key] = encode(parse(prefs[key].orEmpty()).filter { it.appWidgetId != appWidgetId })
        }
    }

    suspend fun snapshot(): List<PinnedWidget> = widgets.first()

    private fun parse(raw: String): List<PinnedWidget> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(PinnedWidget(obj.getInt("id"), obj.optString("provider")))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encode(items: List<PinnedWidget>): String {
        val array = JSONArray()
        items.forEach { widget ->
            array.put(JSONObject().put("id", widget.appWidgetId).put("provider", widget.provider))
        }
        return array.toString()
    }
}
