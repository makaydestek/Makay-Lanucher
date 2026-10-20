package com.makay.launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoBackupStore(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "auto-backups").also { it.mkdirs() }

    suspend fun save(json: String) = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "makay-$stamp.json")
        file.writeText(json)
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(8)?.forEach { it.delete() }
    }

    fun list(): List<File> =
        dir.listFiles()?.filter { it.extension.equals("json", true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    suspend fun read(file: File): String = withContext(Dispatchers.IO) {
        file.readText()
    }
}
