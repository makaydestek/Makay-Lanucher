package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SideloadInstaller(private val context: Context) {
    suspend fun downloadAndInstall(url: String): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            require(url.startsWith("https://") || url.startsWith("http://")) { "Geçersiz URL" }
            val file = File(context.cacheDir, "sideload-${System.currentTimeMillis()}.apk")
            HttpGet.download(url, file)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.update", file)
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
