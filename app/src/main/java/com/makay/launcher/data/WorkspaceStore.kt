package com.makay.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makay.launcher.domain.WorkspaceItem
import com.makay.launcher.domain.WorkspaceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.workspaceDataStore by preferencesDataStore("workspace")

class WorkspaceStore(private val context: Context) {
    private val key = stringPreferencesKey("items")

    val items: Flow<List<WorkspaceItem>> = context.workspaceDataStore.data.map {
        decode(it[key].orEmpty())
    }

    suspend fun snapshot(): List<WorkspaceItem> = items.first()

    suspend fun replace(items: List<WorkspaceItem>) {
        context.workspaceDataStore.edit { it[key] = encode(items) }
    }

    suspend fun update(transform: (List<WorkspaceItem>) -> List<WorkspaceItem>) {
        context.workspaceDataStore.edit { prefs ->
            prefs[key] = encode(transform(decode(prefs[key].orEmpty())))
        }
    }

    suspend fun migrateIfEmpty(homeKeys: List<String>, columns: Int) {
        context.workspaceDataStore.edit { prefs ->
            val current = decode(prefs[key].orEmpty())
            if (current.isNotEmpty() || homeKeys.isEmpty()) return@edit
            val placed = homeKeys.mapIndexed { index, appKey ->
                WorkspaceItem(
                    id = UUID.randomUUID().toString(),
                    type = WorkspaceType.APP,
                    page = index / (columns * 5),
                    cellX = index % columns,
                    cellY = (index / columns) % 5,
                    appKey = appKey,
                )
            }
            prefs[key] = encode(placed)
        }
    }

    private fun encode(items: List<WorkspaceItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
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
                    .put("shortcutId", item.shortcutId),
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<WorkspaceItem> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val members = obj.optJSONArray("members")
                    add(
                        WorkspaceItem(
                            id = obj.getString("id"),
                            type = runCatching { WorkspaceType.valueOf(obj.getString("type")) }
                                .getOrDefault(WorkspaceType.APP),
                            page = obj.optInt("page"),
                            cellX = obj.optInt("cellX"),
                            cellY = obj.optInt("cellY"),
                            spanX = obj.optInt("spanX", 1).coerceAtLeast(1),
                            spanY = obj.optInt("spanY", 1).coerceAtLeast(1),
                            appKey = obj.optString("appKey"),
                            title = obj.optString("title"),
                            members = buildList {
                                if (members != null) {
                                    for (m in 0 until members.length()) add(members.optString(m))
                                }
                            },
                            widgetId = obj.optInt("widgetId", -1),
                            provider = obj.optString("provider"),
                            color = obj.optInt("color"),
                            shortcutId = obj.optString("shortcutId"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object WorkspaceEngine {
    fun firstFree(
        items: List<WorkspaceItem>,
        page: Int,
        columns: Int,
        rows: Int,
        spanX: Int = 1,
        spanY: Int = 1,
    ): Pair<Int, Int>? {
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                if (fits(items, page, x, y, spanX, spanY, ignoreId = null, columns, rows)) return x to y
            }
        }
        return null
    }

    fun fits(
        items: List<WorkspaceItem>,
        page: Int,
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int,
        ignoreId: String?,
        columns: Int = 12,
        rows: Int = 12,
    ): Boolean {
        if (cellX < 0 || cellY < 0 || cellX + spanX > columns || cellY + spanY > rows) return false
        return items.none { item ->
            item.id != ignoreId && item.page == page && overlaps(item, cellX, cellY, spanX, spanY)
        }
    }

    fun occupant(
        items: List<WorkspaceItem>,
        page: Int,
        cellX: Int,
        cellY: Int,
        ignoreId: String?,
    ): WorkspaceItem? = items.firstOrNull { item ->
        item.id != ignoreId && item.page == page && overlaps(item, cellX, cellY, 1, 1)
    }

    fun overlaps(item: WorkspaceItem, x: Int, y: Int, spanX: Int, spanY: Int): Boolean {
        val ax1 = item.cellX
        val ay1 = item.cellY
        val ax2 = item.cellX + item.spanX
        val ay2 = item.cellY + item.spanY
        return x < ax2 && x + spanX > ax1 && y < ay2 && y + spanY > ay1
    }

    fun drop(
        items: List<WorkspaceItem>,
        draggingId: String,
        page: Int,
        cellX: Int,
        cellY: Int,
        columns: Int,
        rows: Int,
    ): List<WorkspaceItem> {
        val drag = items.find { it.id == draggingId } ?: return items
        val target = occupant(items, page, cellX, cellY, draggingId)
        return when {
            target == null && fits(items, page, cellX, cellY, drag.spanX, drag.spanY, draggingId, columns, rows) -> {
                items.map { if (it.id == draggingId) it.copy(page = page, cellX = cellX, cellY = cellY) else it }
            }
            target?.type == WorkspaceType.APP && drag.type == WorkspaceType.APP -> {
                val folder = WorkspaceItem(
                    id = UUID.randomUUID().toString(),
                    type = WorkspaceType.FOLDER,
                    page = target.page,
                    cellX = target.cellX,
                    cellY = target.cellY,
                    title = "Klasör",
                    members = listOfNotNull(target.appKey, drag.appKey).distinct(),
                )
                items.filter { it.id != drag.id && it.id != target.id } + folder
            }
            target?.type == WorkspaceType.FOLDER && drag.type == WorkspaceType.APP -> {
                val updated = target.copy(members = (target.members + drag.appKey).distinct())
                items.filter { it.id != drag.id }.map { if (it.id == target.id) updated else it }
            }
            else -> {
                val free = firstFree(items.filter { it.id != draggingId }, page, columns, rows, drag.spanX, drag.spanY)
                    ?: return items
                items.map {
                    if (it.id == draggingId) it.copy(page = page, cellX = free.first, cellY = free.second) else it
                }
            }
        }
    }

    fun reflow(items: List<WorkspaceItem>, columns: Int, rows: Int): List<WorkspaceItem> {
        val placed = mutableListOf<WorkspaceItem>()
        items.sortedWith(compareBy({ it.page }, { it.cellY }, { it.cellX })).forEach { item ->
            val spanX = item.spanX.coerceIn(1, columns)
            val spanY = item.spanY.coerceIn(1, rows)
            val candidate = item.copy(spanX = spanX, spanY = spanY)
            if (fits(placed, candidate.page, candidate.cellX, candidate.cellY, spanX, spanY, candidate.id, columns, rows)) {
                placed += candidate
            } else {
                var page = candidate.page
                var moved: WorkspaceItem? = null
                while (page < candidate.page + 20) {
                    val free = firstFree(placed, page, columns, rows, spanX, spanY)
                    if (free != null) {
                        moved = candidate.copy(page = page, cellX = free.first, cellY = free.second)
                        break
                    }
                    page++
                }
                if (moved != null) placed += moved
            }
        }
        return placed
    }
}
