package com.makay.launcher.data

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.IconShape

class IconCache(
    context: Context,
    private val iconPackEngine: IconPackEngine,
) {
    private val appContext = context.applicationContext
    private val iconSize = (48f * appContext.resources.displayMetrics.density).toInt()
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(
        app: AppInfo,
        info: LauncherActivityInfo?,
        iconPackPackage: String,
        shape: IconShape = IconShape.SQUIRCLE,
    ): Bitmap {
        val cacheKey = "${iconPackPackage}::${shape.name}::${app.key}"
        cache.get(cacheKey)?.let { return it }
        val packed = iconPackEngine.loadIcon(iconPackPackage, app.packageName, app.activityName)
        val drawable = packed
            ?: info?.getIcon(0)
            ?: appContext.packageManager.getApplicationIcon(app.packageName)
        val bitmap = mask(drawable.toSquareBitmap(iconSize), shape)
        cache.put(cacheKey, bitmap)
        return bitmap
    }

    fun evict(packageName: String) {
        val keys = cache.snapshot().keys.filter { it.contains("$packageName/") }
        keys.forEach { cache.remove(it) }
    }

    fun clear() = cache.evictAll()

    fun trim(level: Int) {
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> clear()
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                val keys = cache.snapshot().keys.toList()
                keys.take(keys.size / 2).forEach { cache.remove(it) }
            }
        }
    }

    private fun mask(source: Bitmap, shape: IconShape): Bitmap {
        val size = source.width
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val path = Path()
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        when (shape) {
            IconShape.CIRCLE -> path.addOval(rect, Path.Direction.CW)
            IconShape.SQUIRCLE -> path.addRoundRect(rect, size * 0.32f, size * 0.32f, Path.Direction.CW)
            IconShape.ROUNDED -> path.addRoundRect(rect, size * 0.18f, size * 0.18f, Path.Direction.CW)
            IconShape.TEARDROP -> {
                val r = size * 0.34f
                path.addRoundRect(rect, floatArrayOf(r, r, r, r, r * 1.4f, r * 1.4f, r, r), Path.Direction.CW)
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(path, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }
}

private fun Drawable.toSquareBitmap(size: Int): Bitmap {
    if (this is BitmapDrawable && bitmap != null && !bitmap.isRecycled) {
        return Bitmap.createScaledBitmap(bitmap, size, size, true)
    }
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, size, size)
    draw(canvas)
    return bitmap
}
