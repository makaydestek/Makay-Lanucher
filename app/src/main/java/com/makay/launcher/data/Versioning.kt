package com.makay.launcher.data

object Versioning {
    fun bumpPatch(name: String, code: Int): Pair<String, Int> {
        val parts = name.split('.').map { it.trim() }.toMutableList()
        while (parts.size < 3) parts.add("0")
        val patch = (parts[2].toIntOrNull() ?: 0) + 1
        parts[2] = patch.toString()
        return parts.take(3).joinToString(".") to (code + 1).coerceAtLeast(1)
    }
}
