package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import com.makay.launcher.domain.AppCategory
import com.makay.launcher.domain.AppInfo

class TvChannelPublisher(private val context: Context) {
    fun publish(apps: List<AppInfo>) {
        if (!isTelevision()) return
        runCatching {
            publishChannel(
                name = CHANNEL_APPS,
                description = "Makay sık kullanılanlar",
                apps = apps.distinctBy { it.packageName }.take(10),
            )
        }
        runCatching {
            val media = apps.filter { it.category == AppCategory.Media || it.category == AppCategory.Games }
                .distinctBy { it.packageName }
                .take(8)
            if (media.isNotEmpty()) {
                publishChannel(
                    name = CHANNEL_WATCH,
                    description = "Medya ve oyun — izlemeye devam",
                    apps = media,
                )
            }
        }
    }

    private fun publishChannel(name: String, description: String, apps: List<AppInfo>) {
        val helper = PreviewChannelHelper(context)
        val existing = runCatching { helper.allChannels }.getOrDefault(emptyList())
            .firstOrNull { it.displayName == name }
        val channel = PreviewChannel.Builder()
            .setDisplayName(name)
            .setDescription(description)
            .setAppLinkIntentUri(Uri.parse("https://github.com/makaydestek/Makay-Lanucher"))
            .build()
        val channelId = existing?.id ?: helper.publishChannel(channel)
        val programUri = android.media.tv.TvContract.buildPreviewProgramsUriForChannel(channelId)
        context.contentResolver.query(programUri, arrayOf("_id"), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                runCatching { helper.deletePreviewProgram(cursor.getLong(0)) }
            }
        }
        apps.forEachIndexed { index, app ->
            val launch = launchIntent(app)
            val subtitle = when {
                name == CHANNEL_WATCH -> "İzlemeye devam · açılış ${app.usageCount}"
                app.category == AppCategory.Games -> "Oyun"
                app.category == AppCategory.Media -> "Medya"
                else -> app.category.title
            }
            val program = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setTitle(app.label)
                .setDescription(subtitle)
                .setIntent(launch)
                .setWeight(1000 - index)
                .build()
            helper.publishPreviewProgram(program)
        }
    }

    private fun launchIntent(app: AppInfo): Intent {
        val launch = context.packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: context.packageManager.getLaunchIntentForPackage(app.packageName)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(app.packageName)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launch
    }

    private fun isTelevision(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            pm.hasSystemFeature("android.software.leanback_only")
    }

    companion object {
        private const val CHANNEL_APPS = "Makay Launcher"
        private const val CHANNEL_WATCH = "İzlemeye devam"
    }
}
