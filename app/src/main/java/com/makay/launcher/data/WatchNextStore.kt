package com.makay.launcher.data

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.watchNextDataStore by preferencesDataStore("watch_next")

data class WatchProgress(
    val packageName: String,
    val title: String = "",
    val detail: String = "",
    val progress: Float = 0f,
    val previewUri: String = "",
    val updatedAt: Long = 0L,
)

class WatchNextStore(private val context: Context) {
    private val key = stringPreferencesKey("progress_json")

    suspend fun snapshot(): Map<String, WatchProgress> {
        val stored = parse(context.watchNextDataStore.data.map { it[key].orEmpty() }.first())
        val live = liveSessions()
        return (stored.keys + live.keys).associateWith { pkg ->
            val a = stored[pkg]
            val b = live[pkg]
            when {
                a == null -> b!!
                b == null -> a
                b.updatedAt >= a.updatedAt -> b
                else -> a
            }
        }
    }

    suspend fun record(packageName: String, title: String = "", progress: Float = 0f, detail: String = "", previewUri: String = "") {
        context.watchNextDataStore.edit { prefs ->
            val map = parse(prefs[key].orEmpty()).toMutableMap()
            val prev = map[packageName]
            map[packageName] = WatchProgress(
                packageName = packageName,
                title = title.ifBlank { prev?.title.orEmpty() },
                detail = detail.ifBlank { prev?.detail.orEmpty() },
                progress = progress.coerceIn(0f, 1f).takeIf { it > 0f } ?: ((prev?.progress ?: 0.2f) + 0.05f).coerceAtMost(0.95f),
                previewUri = previewUri.ifBlank { prev?.previewUri.orEmpty() },
                updatedAt = System.currentTimeMillis(),
            )
            prefs[key] = encode(map)
        }
    }

    fun liveSessions(): Map<String, WatchProgress> {
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return emptyMap()
        return runCatching {
            manager.getActiveSessions(null).mapNotNull { controller ->
                toProgress(controller)
            }.associateBy { it.packageName }
        }.getOrDefault(emptyMap())
    }

    private fun toProgress(controller: MediaController): WatchProgress? {
        val pkg = controller.packageName ?: return null
        val meta = controller.metadata
        val state = controller.playbackState
        val duration = meta?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = state?.position ?: 0L
        val progress = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else {
            if (state?.state == PlaybackState.STATE_PLAYING) 0.45f else 0.2f
        }
        val title = meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val art = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ART_URI)
            ?: meta?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: ""
        return WatchProgress(
            packageName = pkg,
            title = title,
            detail = listOf(artist, if (duration > 0) formatDuration(duration) else "").filter { it.isNotBlank() }.joinToString(" · "),
            progress = progress,
            previewUri = art,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun encode(map: Map<String, WatchProgress>): String {
        val json = JSONObject()
        map.forEach { (pkg, p) ->
            json.put(
                pkg,
                JSONObject()
                    .put("title", p.title)
                    .put("detail", p.detail)
                    .put("progress", p.progress.toDouble())
                    .put("previewUri", p.previewUri)
                    .put("updatedAt", p.updatedAt),
            )
        }
        return json.toString()
    }

    private fun parse(raw: String): Map<String, WatchProgress> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { pkg ->
                val obj = json.getJSONObject(pkg)
                WatchProgress(
                    packageName = pkg,
                    title = obj.optString("title"),
                    detail = obj.optString("detail"),
                    progress = obj.optDouble("progress").toFloat(),
                    previewUri = obj.optString("previewUri"),
                    updatedAt = obj.optLong("updatedAt"),
                )
            }
        }.getOrDefault(emptyMap())
    }
}
