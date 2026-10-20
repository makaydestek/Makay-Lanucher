package com.makay.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makay.launcher.domain.TvAppOverride
import com.makay.launcher.domain.TvCardIconMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.tvOverridesDataStore by preferencesDataStore("tv_overrides")

class TvAppOverridesStore(private val context: Context) {
    private val key = stringPreferencesKey("overrides_json")
    private val focusKey = stringPreferencesKey("row_focus_json")

    val overrides: Flow<Map<String, TvAppOverride>> = context.tvOverridesDataStore.data.map { prefs ->
        parse(prefs[key].orEmpty())
    }

    suspend fun snapshot(): Map<String, TvAppOverride> = overrides.first()

    suspend fun upsert(override: TvAppOverride) {
        context.tvOverridesDataStore.edit { prefs ->
            val map = parse(prefs[key].orEmpty()).toMutableMap()
            if (override.customLabel.isBlank() &&
                override.customIconUri.isBlank() &&
                override.previewUri.isBlank() &&
                override.iconMode == TvCardIconMode.ICON
            ) {
                map.remove(override.appKey)
            } else {
                map[override.appKey] = override
            }
            prefs[key] = encode(map)
        }
    }

    suspend fun rememberFocus(rowId: String, appKey: String) {
        context.tvOverridesDataStore.edit { prefs ->
            val json = runCatching { JSONObject(prefs[focusKey].orEmpty()) }.getOrDefault(JSONObject())
            json.put(rowId, appKey)
            prefs[focusKey] = json.toString()
        }
    }

    suspend fun focusedKeys(): Map<String, String> {
        val raw = context.tvOverridesDataStore.data.first()[focusKey].orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.optString(it) }
        }.getOrDefault(emptyMap())
    }

    fun encode(map: Map<String, TvAppOverride>): String {
        val array = JSONArray()
        map.values.forEach { o ->
            array.put(
                JSONObject()
                    .put("appKey", o.appKey)
                    .put("customLabel", o.customLabel)
                    .put("customIconUri", o.customIconUri)
                    .put("previewUri", o.previewUri)
                    .put("iconMode", o.iconMode.name),
            )
        }
        return array.toString()
    }

    fun parse(raw: String): Map<String, TvAppOverride> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val key = obj.optString("appKey")
                    if (key.isBlank()) continue
                    put(
                        key,
                        TvAppOverride(
                            appKey = key,
                            customLabel = obj.optString("customLabel"),
                            customIconUri = obj.optString("customIconUri"),
                            previewUri = obj.optString("previewUri"),
                            iconMode = runCatching {
                                TvCardIconMode.valueOf(obj.optString("iconMode", TvCardIconMode.ICON.name))
                            }.getOrDefault(TvCardIconMode.ICON),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }
}
