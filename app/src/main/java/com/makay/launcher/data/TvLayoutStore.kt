package com.makay.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makay.launcher.domain.TvBuiltinRow
import com.makay.launcher.domain.TvRowDef
import com.makay.launcher.domain.TvRowSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.tvLayoutDataStore by preferencesDataStore("tv_layout")

class TvLayoutStore(private val context: Context) {
    private val key = stringPreferencesKey("rows_json")

    val rows: Flow<List<TvRowDef>> = context.tvLayoutDataStore.data.map { prefs ->
        parse(prefs[key].orEmpty()).ifEmpty { TvRowEngine.defaultLayout() }
    }

    suspend fun snapshot(): List<TvRowDef> = rows.first()

    suspend fun replace(next: List<TvRowDef>) {
        context.tvLayoutDataStore.edit { prefs ->
            prefs[key] = encode(next)
        }
    }

    suspend fun update(transform: (List<TvRowDef>) -> List<TvRowDef>) {
        replace(transform(snapshot()))
    }

    fun encode(rows: List<TvRowDef>): String {
        val array = JSONArray()
        rows.forEach { row ->
            array.put(
                JSONObject()
                    .put("id", row.id)
                    .put("builtin", row.builtin?.name.orEmpty())
                    .put("title", row.title)
                    .put("visible", row.visible)
                    .put("sort", row.sort.name)
                    .put("maxItems", row.maxItems)
                    .put("isDefault", row.isDefault)
                    .put("appKeys", JSONArray(row.appKeys)),
            )
        }
        return array.toString()
    }

    fun parse(raw: String): List<TvRowDef> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val builtinName = obj.optString("builtin")
                    val keys = obj.optJSONArray("appKeys")
                    add(
                        TvRowDef(
                            id = obj.optString("id").ifBlank { "row-$i" },
                            builtin = builtinName.takeIf { it.isNotBlank() }?.let {
                                runCatching { TvBuiltinRow.valueOf(it) }.getOrNull()
                            },
                            title = obj.optString("title"),
                            visible = obj.optBoolean("visible", true),
                            sort = runCatching {
                                TvRowSort.valueOf(obj.optString("sort", TvRowSort.USAGE.name))
                            }.getOrDefault(TvRowSort.USAGE),
                            maxItems = obj.optInt("maxItems"),
                            isDefault = obj.optBoolean("isDefault"),
                            appKeys = buildList {
                                if (keys != null) for (k in 0 until keys.length()) add(keys.optString(k))
                            },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
