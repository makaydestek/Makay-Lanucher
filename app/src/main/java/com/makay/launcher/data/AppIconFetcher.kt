package com.makay.launcher.data

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.IconShape

data class AppIconKey(
    val app: AppInfo,
    val pack: String,
    val shape: IconShape,
)

class AppIconFetcher(
    private val key: AppIconKey,
    private val cache: IconCache,
    private val apps: InstalledAppsStore,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bitmap = cache.get(key.app, apps.activityInfo(key.app), key.pack, key.shape)
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory(
        private val cache: IconCache,
        private val apps: InstalledAppsStore,
    ) : Fetcher.Factory<AppIconKey> {
        override fun create(data: AppIconKey, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, cache, apps)
        }
    }
}
