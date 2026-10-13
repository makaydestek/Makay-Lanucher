package com.makay.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WallpaperPalette {
    private val _generation = MutableStateFlow(0)
    val generation: StateFlow<Int> = _generation

    fun bump() {
        _generation.value = _generation.value + 1
    }

    fun start(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        val manager = WallpaperManager.getInstance(context)
        runCatching {
            manager.addOnColorsChangedListener(
                { _, _ -> bump() },
                Handler(Looper.getMainLooper()),
            )
        }
        bump()
    }
}
