package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import com.makay.launcher.BuildConfig
import com.makay.launcher.domain.LauncherSettings
import com.makay.launcher.domain.UpdateOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class UpdateCheckResult {
    data class NewUpdate(val offer: UpdateOffer) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    object NoUrl : UpdateCheckResult()
    object Failed : UpdateCheckResult()
    object Postponed : UpdateCheckResult()
}

class UpdateManager(private val context: Context) {

    suspend fun check(
        settings: LauncherSettings,
        force: Boolean,
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = settings.updateUrl
            .ifBlank { BuildConfig.UPDATE_URL }
            .ifBlank { HttpGet.MANIFEST_URL }

        if (url.isBlank()) {
            return@withContext UpdateCheckResult.NoUrl
        }

        val offer = UpdateChecker.fetch(url) ?: return@withContext UpdateCheckResult.Failed

        if (offer.versionCode <= BuildConfig.VERSION_CODE) {
            return@withContext UpdateCheckResult.UpToDate
        }

        val now = System.currentTimeMillis()
        if (!force &&
            settings.postponedVersionCode == offer.versionCode &&
            settings.postponedUntil > now
        ) {
            return@withContext UpdateCheckResult.Postponed
        }

        return@withContext UpdateCheckResult.NewUpdate(offer)
    }

    suspend fun downloadAndPrepareInstall(
        offer: UpdateOffer,
        onStatus: (String) -> Unit,
    ): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            onStatus("İndiriliyor…")
            val file = UpdateInstaller.download(context, offer.apkUrl, onStatus)
            UpdateInstaller.verify(file, offer.sha256, offer.apkUrl, onStatus)
            UpdateInstaller.installIntent(context, file)
        }
    }
}
