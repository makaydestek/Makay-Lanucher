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

    suspend fun snapshot(): Map<String, Int> {
        val raw = context.usageDataStore.data.map { it[key].orEmpty() }.first()
        val local = parse(raw)
        val system = SystemUsage.counts(context)
        return (local.keys + system.keys).associateWith { pkg ->
            (local[pkg] ?: 0) + (system[pkg] ?: 0)
        }
    }

    fun record(packageName: String) {
        scope.launch {
            context.usageDataStore.edit { prefs ->
                val map = parse(prefs[key].orEmpty()).toMutableMap()
                map[packageName] = (map[packageName] ?: 0) + 1
                val json = JSONObject()
                map.forEach { (k, v) -> json.put(k, v) }
                prefs[key] = json.toString()
            }
        }
    }

    private fun parse(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.optInt(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
