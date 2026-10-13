package com.makay.launcher.data

import android.graphics.Color
import android.graphics.Typeface
import android.service.dreams.DreamService
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MakayDreamService : DreamService() {
    private val clock = SimpleDateFormat("HH:mm", Locale("tr"))
    private val date = SimpleDateFormat("EEEE d MMMM", Locale("tr"))

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = false
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0D1613"))
            setPadding(48, 48, 48, 48)
        }
        val timeView = TextView(this).apply {
            setTextColor(Color.parseColor("#F4F1EA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 72f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            text = clock.format(Date())
        }
        val dateView = TextView(this).apply {
            setTextColor(Color.parseColor("#C6A15B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            text = date.format(Date()).replaceFirstChar { it.uppercase() }
        }
        val brand = TextView(this).apply {
            setTextColor(Color.parseColor("#8FBFB0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            text = "Makay Launcher"
        }
        root.addView(timeView)
        root.addView(dateView)
        root.addView(brand)
        setContentView(root)
        root.postDelayed(object : Runnable {
            override fun run() {
                timeView.text = clock.format(Date())
                dateView.text = date.format(Date()).replaceFirstChar { it.uppercase() }
                root.postDelayed(this, 15_000)
            }
        }, 15_000)
    }
}
