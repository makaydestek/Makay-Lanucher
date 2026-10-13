package com.makay.launcher.data

import java.io.File
import java.security.MessageDigest

object ApkHasher {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    fun parseSidecar(raw: String): String =
        raw.trim().lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
            ?.substringBefore(' ')
            ?.substringBefore('\t')
            ?.lowercase()
            .orEmpty()

    fun matches(actual: String, expected: String): Boolean {
        val left = actual.trim().lowercase()
        val right = expected.trim().lowercase()
        return left.length == 64 && left == right
    }
}
