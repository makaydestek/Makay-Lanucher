package com.makay.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class WallpaperFetcher(private val context: Context) {
    suspend fun applyBingDaily(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = HttpGet.text("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=tr-TR")
            val imagePath = JSONObject(json)
                .getJSONArray("images")
                .getJSONObject(0)
                .getString("url")
            val imageUrl = "https://www.bing.com$imagePath"
            val file = File.createTempFile("bing", ".img", context.cacheDir)
            try {
                HttpGet.download(imageUrl, file)
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: error("Görsel çözümlenemedi")
                WallpaperManager.getInstance(context).setBitmap(bitmap)
                bitmap.recycle()
                WallpaperPalette.bump()
            } finally {
                file.delete()
            }
        }
    }
}
