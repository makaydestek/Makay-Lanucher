package com.makay.launcher.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class WeatherStore(private val context: Context) {
    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()
    @Volatile private var cachedWeather: String = ""
    @Volatile private var lastFetchTimeMs: Long = 0L
    private var inflight: Deferred<String>? = null
    private val cacheDurationMs: Long = 15 * 60 * 1000L

    @SuppressLint("MissingPermission")
    suspend fun current(): String {
        val now = System.currentTimeMillis()
        if (cachedWeather.isNotBlank() && now - lastFetchTimeMs < cacheDurationMs) return cachedWeather
        val pending = gate.withLock {
            val lockedNow = System.currentTimeMillis()
            if (cachedWeather.isNotBlank() && lockedNow - lastFetchTimeMs < cacheDurationMs) return@withLock null
            inflight?.takeIf { it.isActive } ?: fetchScope.async { load() }.also { inflight = it }
        }
        return pending?.await() ?: cachedWeather
    }

    @SuppressLint("MissingPermission")
    private fun load(): String {
        return runCatching {
            val location = resolveBestLocation() ?: return@runCatching cachedWeather
            val url = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}" +
                "&longitude=${location.longitude}&current=temperature_2m,weather_code&timezone=auto"
            val json = HttpGet.text(url, timeoutMs = 6_000)
            val currentObj = JSONObject(json).getJSONObject("current")
            val temp = currentObj.optDouble("temperature_2m")
            val code = currentObj.optInt("weather_code")
            val result = "${temp.toInt()}° · ${label(code)}"
            cachedWeather = result
            lastFetchTimeMs = System.currentTimeMillis()
            result
        }.getOrDefault(cachedWeather)
    }

    @SuppressLint("MissingPermission")
    private fun resolveBestLocation(): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(
            "fused",
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (bestLocation == null || loc.time > bestLocation.time) {
                bestLocation = loc
            }
        }
        return bestLocation
    }

    private fun label(code: Int): String = when (code) {
        0 -> "Açık"
        1, 2, 3 -> "Parçalı bulutlu"
        45, 48 -> "Sis"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Yağmur"
        71, 73, 75, 85, 86 -> "Kar"
        95, 96, 99 -> "Fırtına"
        else -> "Hava"
    }
}
