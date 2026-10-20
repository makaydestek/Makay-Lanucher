package com.makay.launcher.domain

/** TV ana ekran satır sırası, kart görünümü, wallpaper, idle/boot ve ebeveyn kontrolü. */

enum class TvRowSort {
    ALPHA,
    LAST_USED,
    USAGE,
    RANDOM,
    MANUAL,
}

enum class TvBuiltinRow(val defaultTitle: String) {
    ACTIONS("Hızlı erişim"),
    LETTERS("Harfle ara"),
    WORK("İş profili"),
    PRIVATE("Özel alan"),
    INPUTS("Giriş kaynakları"),
    FREQUENT("Sık kullanılanlar"),
    FAVORITES("Favoriler"),
    MEDIA("Medya ve oyun"),
    CAT_SOCIAL("Sosyal"),
    CAT_COMMUNICATION("İletişim"),
    CAT_MEDIA("Medya"),
    CAT_GAMES("Oyun"),
    CAT_PRODUCTIVITY("Üretkenlik"),
    CAT_TOOLS("Araçlar"),
    CAT_SYSTEM("Sistem"),
    CAT_OTHER("Diğer"),
    WATCH_NEXT("İzlemeye devam"),
    NOW_PLAYING("Şimdi oynatılıyor"),
}

enum class TvCardSize(val widthDp: Int, val iconDp: Int) {
    SMALL(132, 56),
    MEDIUM(168, 72),
    LARGE(208, 88),
}

enum class TvFocusEffect {
    SCALE,
    GLOW,
    SHADOW,
    NONE,
}

enum class TvWallpaperKind {
    NONE,
    IMAGE,
    GIF,
    VIDEO,
    URL,
    RTSP,
    BING,
    FOCUSED,
}

enum class TvIdleAction {
    NONE,
    SCREENSAVER,
    DIM,
    STANDBY,
}

enum class TvCalibrationPattern {
    COLOR_BARS,
    GRID,
    GRAYSCALE,
    JUDDER,
    HDR_CLIP,
    DOLBY_BARS,
}

enum class TvCardIconMode {
    ICON,
    BANNER,
}

enum class TvFamilySlot(val title: String, val startMin: Int, val endMin: Int) {
    BEDTIME("Yatma", 21 * 60, 7 * 60),
    HOMEWORK("Ödev", 16 * 60, 19 * 60),
    MORNING("Sabah", 6 * 60, 8 * 60),
}

data class TvRowDef(
    val id: String,
    val builtin: TvBuiltinRow? = null,
    val title: String = "",
    val visible: Boolean = true,
    val sort: TvRowSort = TvRowSort.USAGE,
    val maxItems: Int = 0,
    val isDefault: Boolean = false,
    val appKeys: List<String> = emptyList(),
) {
    val displayTitle: String
        get() = title.ifBlank { builtin?.defaultTitle.orEmpty().ifBlank { "Satır" } }
}

data class TvResolvedRow(
    val def: TvRowDef,
    val apps: List<AppInfo> = emptyList(),
    val inputs: List<InputSource> = emptyList(),
    val watchItems: List<WatchNextItem> = emptyList(),
    val isActions: Boolean = false,
    val isLetters: Boolean = false,
    val isNowPlaying: Boolean = false,
)

data class WatchNextItem(
    val app: AppInfo,
    val subtitle: String = "",
    val progress: Float = 0f,
    val previewUri: String = "",
    val detail: String = "",
)

data class TvAppOverride(
    val appKey: String,
    val customLabel: String = "",
    val customIconUri: String = "",
    val previewUri: String = "",
    val iconMode: TvCardIconMode = TvCardIconMode.ICON,
)

data class NowPlayingInfo(
    val packageName: String = "",
    val title: String = "",
    val artist: String = "",
    val progress: Float = 0f,
    val artUri: String = "",
)

data class ProgramDetail(
    val app: AppInfo,
    val title: String,
    val subtitle: String,
    val detail: String,
    val progress: Float,
    val previewUri: String = "",
)
