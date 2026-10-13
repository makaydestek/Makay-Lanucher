package com.makay.launcher.data

object FavoriteOrder {
    fun encode(keys: List<String>): Set<String> =
        keys.mapIndexed { index, key -> "$index::$key" }.toSet()

    fun decode(raw: Set<String>?): List<String> =
        raw.orEmpty()
            .mapNotNull {
                val parts = it.split("::", limit = 2)
                if (parts.size == 2) parts[0].toIntOrNull() to parts[1] else null
            }
            .sortedBy { it.first }
            .map { it.second }

    fun move(keys: List<String>, key: String, delta: Int): List<String> {
        val current = keys.toMutableList()
        val index = current.indexOf(key)
        if (index < 0) return keys
        val next = (index + delta).coerceIn(0, current.lastIndex)
        if (next == index) return keys
        val item = current.removeAt(index)
        current.add(next, item)
        return current
    }
}
