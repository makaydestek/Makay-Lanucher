package com.makay.launcher.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

object WallpaperProviders {
    /** Bing günlük görsel URL’si (indirmeden wallpaper katmanında kullanılır). */
    suspend fun bingImageUrl(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val json = HttpGet.text("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=tr-TR")
            val path = JSONObject(json).getJSONArray("images").getJSONObject(0).getString("url")
            "https://www.bing.com$path"
        }
    }

    fun isStreamUrl(uri: String): Boolean {
        val lower = uri.lowercase()
        return lower.startsWith("rtsp://") ||
            lower.startsWith("rtsps://") ||
            lower.endsWith(".m3u8") ||
            lower.contains(".mp4") ||
            lower.contains(".webm")
    }

    fun isImageUrl(uri: String): Boolean {
        val lower = uri.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".webp") ||
            lower.endsWith(".gif") || lower.contains("bing.com")
    }

    suspend fun cacheRemoteImage(context: Context, url: String): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.cacheDir, "tv_wp_${url.hashCode()}.img")
            if (!file.exists() || file.length() == 0L || System.currentTimeMillis() - file.lastModified() > 6 * 60 * 60 * 1000L) {
                HttpGet.download(url, file)
            }
            Uri.fromFile(file)
        }.getOrNull()
    }
}
