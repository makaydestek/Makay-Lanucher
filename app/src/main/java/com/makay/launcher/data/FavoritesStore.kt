package com.makay.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore("favorites")

class FavoritesStore(private val context: Context) {
    private val homeKey = stringSetPreferencesKey("home")
    private val dockKey = stringSetPreferencesKey("dock")
    private val hiddenKey = stringSetPreferencesKey("hidden")
    private val lockedKey = stringSetPreferencesKey("locked")

    val homeKeys: Flow<List<String>> = context.favoritesDataStore.data.map { prefs ->
        FavoriteOrder.decode(prefs[homeKey])
    }
    val dockKeys: Flow<List<String>> = context.favoritesDataStore.data.map { prefs ->
        FavoriteOrder.decode(prefs[dockKey])
    }
    val hiddenKeys: Flow<Set<String>> = context.favoritesDataStore.data.map { prefs ->
        prefs[hiddenKey].orEmpty()
    }
    val lockedKeys: Flow<Set<String>> = context.favoritesDataStore.data.map { prefs ->
        prefs[lockedKey].orEmpty()
    }

    suspend fun setHome(keys: List<String>) {
        context.favoritesDataStore.edit { it[homeKey] = FavoriteOrder.encode(keys) }
    }

    suspend fun setDock(keys: List<String>) {
        context.favoritesDataStore.edit { it[dockKey] = FavoriteOrder.encode(keys) }
    }

    suspend fun toggleDock(key: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = FavoriteOrder.decode(prefs[dockKey]).toMutableList()
            if (current.contains(key)) current.remove(key) else current.add(key)
            prefs[dockKey] = FavoriteOrder.encode(current)
        }
    }

    suspend fun prunePackage(packageName: String) {
        val prefix = "$packageName/"
        context.favoritesDataStore.edit { prefs ->
            prefs[homeKey] = FavoriteOrder.encode(FavoriteOrder.decode(prefs[homeKey]).filterNot { it.startsWith(prefix) })
            prefs[dockKey] = FavoriteOrder.encode(FavoriteOrder.decode(prefs[dockKey]).filterNot { it.startsWith(prefix) })
            prefs[hiddenKey] = prefs[hiddenKey].orEmpty() - packageName
            prefs[lockedKey] = prefs[lockedKey].orEmpty() - packageName
        }
    }

    suspend fun toggleHome(key: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = FavoriteOrder.decode(prefs[homeKey]).toMutableList()
            if (current.contains(key)) current.remove(key) else current.add(key)
            prefs[homeKey] = FavoriteOrder.encode(current)
        }
    }

    suspend fun moveHome(key: String, delta: Int) {
        context.favoritesDataStore.edit { prefs ->
            prefs[homeKey] = FavoriteOrder.encode(FavoriteOrder.move(FavoriteOrder.decode(prefs[homeKey]), key, delta))
        }
    }

    suspend fun toggleHidden(packageName: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[hiddenKey].orEmpty().toMutableSet()
            if (!current.add(packageName)) current.remove(packageName)
            prefs[hiddenKey] = current
        }
    }

    suspend fun toggleLocked(packageName: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[lockedKey].orEmpty().toMutableSet()
            if (!current.add(packageName)) current.remove(packageName)
            prefs[lockedKey] = current
        }
    }

    suspend fun snapshot(): FavoriteSnapshot {
        val prefs = context.favoritesDataStore.data.first()
        return FavoriteSnapshot(
            home = FavoriteOrder.decode(prefs[homeKey]),
            dock = FavoriteOrder.decode(prefs[dockKey]),
            hidden = prefs[hiddenKey].orEmpty(),
            locked = prefs[lockedKey].orEmpty(),
        )
    }

    suspend fun restore(snapshot: FavoriteSnapshot) {
        context.favoritesDataStore.edit { prefs ->
            prefs[homeKey] = FavoriteOrder.encode(snapshot.home)
            prefs[dockKey] = FavoriteOrder.encode(snapshot.dock)
            prefs[hiddenKey] = snapshot.hidden
            prefs[lockedKey] = snapshot.locked
        }
    }

    suspend fun seedDockIfEmpty(defaults: List<String>) {
        context.favoritesDataStore.edit { prefs ->
            if (prefs[dockKey].isNullOrEmpty()) {
                prefs[dockKey] = FavoriteOrder.encode(defaults)
            }
        }
    }

}

data class FavoriteSnapshot(
    val home: List<String>,
    val dock: List<String>,
    val hidden: Set<String>,
    val locked: Set<String>,
)
