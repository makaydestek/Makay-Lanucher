package com.makay.launcher.data

import com.makay.launcher.domain.UpdateOffer
import org.json.JSONObject

object UpdateChecker {
    fun fetch(manifestUrl: String): UpdateOffer? {
        if (manifestUrl.isBlank()) return null
        return runCatching { parse(HttpGet.text(manifestUrl)) }.getOrNull()
    }

    fun parse(raw: String): UpdateOffer? {
        val json = JSONObject(raw)
        return UpdateOffer(
            versionName = json.optString("versionName"),
            versionCode = json.optInt("versionCode"),
            apkUrl = json.optString("apkUrl"),
            notes = json.optString("notes"),
            sha256 = json.optString("sha256").trim().lowercase(),
        ).takeIf { it.versionCode > 0 && it.versionName.isNotBlank() }
    }
}
