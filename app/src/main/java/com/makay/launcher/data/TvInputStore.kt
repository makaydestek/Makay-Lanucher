package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import com.makay.launcher.domain.InputSource

class TvInputStore(private val context: Context) {
    fun sources(): List<InputSource> {
        val manager = context.getSystemService(TvInputManager::class.java) ?: return emptyList()
        return runCatching {
            manager.tvInputList.map { info ->
                InputSource(
                    id = info.id,
                    label = info.loadLabel(context)?.toString()
                        ?: info.id.substringAfterLast("/"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun launch(source: InputSource) {
        val passthrough = runCatching {
            Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(source.id))
        }.getOrNull()
        val intent = passthrough ?: Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("content://android.media.tv/passthrough/${source.id}")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
