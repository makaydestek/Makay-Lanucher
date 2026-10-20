package com.makay.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject

private val Context.usageDataStore by preferencesDataStore("usage")

class UsageStore(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringPreferencesKey("counts")
    private val lastKey = stringPreferencesKey("last_opened")

    suspend fun snapshot(): Map<String, Int> {
        val raw = context.usageDataStore.data.map { it[key].orEmpty() }.first()
        val local = parseCounts(raw)
        val system = SystemUsage.counts(context)
        return (local.keys + system.keys).associateWith { pkg ->
            (local[pkg] ?: 0) + (system[pkg] ?: 0)
        }
    }

    suspend fun lastOpened(): Map<String, Long> {
        val raw = context.usageDataStore.data.map { it[lastKey].orEmpty() }.first()
        return parseLast(raw)
    }

    fun record(packageName: String) {
        scope.launch {
            context.usageDataStore.edit { prefs ->
                val map = parseCounts(prefs[key].orEmpty()).toMutableMap()
                map[packageName] = (map[packageName] ?: 0) + 1
                val json = JSONObject()
                map.forEach { (k, v) -> json.put(k, v) }
                prefs[key] = json.toString()
                val last = parseLast(prefs[lastKey].orEmpty()).toMutableMap()
                last[packageName] = System.currentTimeMillis()
                val lastJson = JSONObject()
                last.forEach { (k, v) -> lastJson.put(k, v) }
                prefs[lastKey] = lastJson.toString()
            }
        }
    }

    private fun parseCounts(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.optInt(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseLast(raw: String): Map<String, Long> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.optLong(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
