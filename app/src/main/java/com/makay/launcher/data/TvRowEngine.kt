package com.makay.launcher.data

import com.makay.launcher.domain.AppCategory
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.InputSource
import com.makay.launcher.domain.TvBuiltinRow
import com.makay.launcher.domain.TvResolvedRow
import com.makay.launcher.domain.TvRowDef
import com.makay.launcher.domain.TvRowSort
import java.util.UUID
import kotlin.random.Random

object TvRowEngine {
    fun defaultLayout(): List<TvRowDef> = listOf(
        TvRowDef(id = "actions", builtin = TvBuiltinRow.ACTIONS, isDefault = true),
        TvRowDef(id = "letters", builtin = TvBuiltinRow.LETTERS),
        TvRowDef(id = "work", builtin = TvBuiltinRow.WORK),
        TvRowDef(id = "private", builtin = TvBuiltinRow.PRIVATE),
        TvRowDef(id = "inputs", builtin = TvBuiltinRow.INPUTS),
        TvRowDef(id = "frequent", builtin = TvBuiltinRow.FREQUENT, sort = TvRowSort.USAGE, maxItems = 14),
        TvRowDef(id = "favorites", builtin = TvBuiltinRow.FAVORITES, sort = TvRowSort.MANUAL),
        TvRowDef(id = "watch", builtin = TvBuiltinRow.WATCH_NEXT, sort = TvRowSort.USAGE, maxItems = 10),
        TvRowDef(id = "media", builtin = TvBuiltinRow.MEDIA, sort = TvRowSort.USAGE),
        TvRowDef(id = "cat_social", builtin = TvBuiltinRow.CAT_SOCIAL),
        TvRowDef(id = "cat_comm", builtin = TvBuiltinRow.CAT_COMMUNICATION),
        TvRowDef(id = "cat_media", builtin = TvBuiltinRow.CAT_MEDIA),
        TvRowDef(id = "cat_games", builtin = TvBuiltinRow.CAT_GAMES),
        TvRowDef(id = "cat_prod", builtin = TvBuiltinRow.CAT_PRODUCTIVITY),
        TvRowDef(id = "cat_tools", builtin = TvBuiltinRow.CAT_TOOLS),
        TvRowDef(id = "cat_system", builtin = TvBuiltinRow.CAT_SYSTEM),
        TvRowDef(id = "cat_other", builtin = TvBuiltinRow.CAT_OTHER),
    )

    fun resolve(
        layout: List<TvRowDef>,
        state: HomeUiState,
        lastUsed: Map<String, Long> = emptyMap(),
        hideAboveFocusedId: String? = null,
    ): List<TvResolvedRow> {
        val rows = layout.ifEmpty { defaultLayout() }
        val focusedIndex = hideAboveFocusedId?.let { id -> rows.indexOfFirst { it.id == id } } ?: -1
        return rows.mapIndexedNotNull { index, def ->
            if (!def.visible) return@mapIndexedNotNull null
            if (focusedIndex > 0 && index < focusedIndex) return@mapIndexedNotNull null
            if (state.settings.digitalDetox && def.builtin == TvBuiltinRow.FREQUENT) {
                return@mapIndexedNotNull null
            }
            buildRow(def, state, lastUsed)
        }
    }

    fun buildRow(def: TvRowDef, state: HomeUiState, lastUsed: Map<String, Long>): TvResolvedRow? {
        return when (def.builtin) {
            TvBuiltinRow.ACTIONS -> TvResolvedRow(def = def, isActions = true)
            TvBuiltinRow.LETTERS -> TvResolvedRow(def = def, isLetters = true)
            TvBuiltinRow.WORK -> appsOrNull(def, state.apps.filter { it.work }, lastUsed)
            TvBuiltinRow.PRIVATE -> appsOrNull(def, state.apps.filter { it.privateSpace }, lastUsed)
            TvBuiltinRow.INPUTS -> {
                if (state.inputSources.isEmpty()) null
                else TvResolvedRow(def = def, inputs = state.inputSources)
            }
            TvBuiltinRow.FREQUENT -> appsOrNull(
                def,
                state.apps.sortedByDescending { it.usageCount }.take(14),
                lastUsed,
            )
            TvBuiltinRow.FAVORITES -> {
                val favorites = state.favorites.ifEmpty {
                    state.apps.sortedByDescending { it.usageCount }.take(12)
                }
                appsOrNull(def, favorites, lastUsed, forceManual = true)
            }
            TvBuiltinRow.MEDIA -> appsOrNull(
                def,
                state.apps.filter { it.category == AppCategory.Media || it.category == AppCategory.Games },
                lastUsed,
            )
            TvBuiltinRow.WATCH_NEXT -> appsOrNull(
                def,
                state.apps.filter { it.category == AppCategory.Media || it.category == AppCategory.Games }
                    .sortedByDescending { it.usageCount },
                lastUsed,
            )
            TvBuiltinRow.CAT_SOCIAL -> category(def, state, AppCategory.Social, lastUsed)
            TvBuiltinRow.CAT_COMMUNICATION -> category(def, state, AppCategory.Communication, lastUsed)
            TvBuiltinRow.CAT_MEDIA -> category(def, state, AppCategory.Media, lastUsed)
            TvBuiltinRow.CAT_GAMES -> category(def, state, AppCategory.Games, lastUsed)
            TvBuiltinRow.CAT_PRODUCTIVITY -> category(def, state, AppCategory.Productivity, lastUsed)
            TvBuiltinRow.CAT_TOOLS -> category(def, state, AppCategory.Tools, lastUsed)
            TvBuiltinRow.CAT_SYSTEM -> category(def, state, AppCategory.System, lastUsed)
            TvBuiltinRow.CAT_OTHER -> category(def, state, AppCategory.Other, lastUsed)
            null -> {
                val mapped = def.appKeys.mapNotNull { key -> state.apps.find { it.key == key } }
                appsOrNull(def, mapped, lastUsed, forceManual = def.sort == TvRowSort.MANUAL)
            }
        }
    }

    fun move(layout: List<TvRowDef>, id: String, delta: Int): List<TvRowDef> {
        val list = layout.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return layout
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from == to) return layout
        val item = list.removeAt(from)
        list.add(to, item)
        return list
    }

    fun setDefault(layout: List<TvRowDef>, id: String): List<TvRowDef> =
        layout.map { it.copy(isDefault = it.id == id) }

    fun newCustom(title: String, appKeys: List<String>): TvRowDef = TvRowDef(
        id = "custom-${UUID.randomUUID()}",
        title = title.ifBlank { "Özel satır" },
        sort = TvRowSort.MANUAL,
        appKeys = appKeys,
    )

    private fun category(
        def: TvRowDef,
        state: HomeUiState,
        category: AppCategory,
        lastUsed: Map<String, Long>,
    ): TvResolvedRow? {
        if (!state.settings.autoCategories) return null
        return appsOrNull(def, state.apps.filter { it.category == category }, lastUsed)
    }

    private fun appsOrNull(
        def: TvRowDef,
        apps: List<AppInfo>,
        lastUsed: Map<String, Long>,
        forceManual: Boolean = false,
    ): TvResolvedRow? {
        if (apps.isEmpty() && def.builtin != null) return null
        val sorted = sortApps(apps, if (forceManual) TvRowSort.MANUAL else def.sort, lastUsed, def.appKeys)
        val limited = if (def.maxItems > 0) sorted.take(def.maxItems) else sorted
        if (limited.isEmpty() && def.builtin != null) return null
        return TvResolvedRow(def = def, apps = limited)
    }

    fun sortApps(
        apps: List<AppInfo>,
        sort: TvRowSort,
        lastUsed: Map<String, Long>,
        manualOrder: List<String> = emptyList(),
    ): List<AppInfo> = when (sort) {
        TvRowSort.ALPHA -> apps.sortedBy { it.label.lowercase() }
        TvRowSort.LAST_USED -> apps.sortedByDescending { lastUsed[it.packageName] ?: 0L }
        TvRowSort.USAGE -> apps.sortedByDescending { it.usageCount }
        TvRowSort.RANDOM -> apps.shuffled(Random(System.currentTimeMillis() / 60_000))
        TvRowSort.MANUAL -> {
            if (manualOrder.isEmpty()) apps
            else {
                val byKey = apps.associateBy { it.key }
                manualOrder.mapNotNull { byKey[it] } + apps.filter { it.key !in manualOrder }
            }
        }
    }
}
