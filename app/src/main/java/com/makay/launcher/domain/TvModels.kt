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
}

enum class TvIdleAction {
    NONE,
    SCREENSAVER,
    DIM,
}

enum class TvCalibrationPattern {
    COLOR_BARS,
    GRID,
    GRAYSCALE,
    JUDDER,
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
    val isActions: Boolean = false,
    val isLetters: Boolean = false,
)

data class WatchNextItem(
    val app: AppInfo,
    val subtitle: String = "",
    val progress: Float = 0f,
)