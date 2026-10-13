package com.makay.launcher.domain

/**
 * Paket adına ve sistem bayrağına göre [AppCategory] atar.
 *
 * Yeni bir kategori eklemek için yalnızca [categoryCatalog] haritasına
 * ilgili anahtar kelimeleri eklemek yeterlidir; [categorize] fonksiyonuna
 * dokunmaya gerek yoktur.
 */
object AppCategorizer {

    /**
     * Her [AppCategory] için eşleşme anahtar kelimeleri.
     * Eşleşme, paket adının söz konusu kelimeyi içermesiyle yapılır.
     */
    private val categoryCatalog: Map<AppCategory, List<String>> = mapOf(
        AppCategory.Social to listOf(
            "whatsapp", "telegram", "instagram", "facebook", "fb", "twitter", "x.android",
            "tiktok", "snapchat", "discord", "messenger", "linkedin", "reddit", "signal",
            "threads", "bereal", "pinterest",
        ),
        AppCategory.Communication to listOf(
            "dialer", "phone", "contacts", "mms", "messaging", "sms", "gm", "gmail",
            "mail", "outlook", "thunderbird", "k9", "skype", "zoom", "meet", "teams",
        ),
        AppCategory.Media to listOf(
            "youtube", "netflix", "spotify", "twitch", "gallery", "photos", "vlc",
            "music", "plex", "disney", "primevideo", "soundcloud", "camera", "fm.player",
        ),
        AppCategory.Games to listOf(
            "game", "games", "roblox", "minecraft", "supercell", "unity", "riot",
            "steam", "epicgames", "play.games", "mojang", "pubg", "freefire",
        ),
        AppCategory.Productivity to listOf(
            "docs", "sheets", "slides", "office", "word", "excel", "onenote", "notion",
            "evernote", "keep", "calendar", "drive", "dropbox", "onedrive", "todoist",
            "tasks", "trello", "slack",
        ),
        AppCategory.Tools to listOf(
            "settings", "calculator", "clock", "files", "filemanager", "documents",
            "chrome", "browser", "firefox", "maps", "weather", "flashlight", "store",
            "vending", "fdroid", "termux",
        ),
        AppCategory.System to listOf(
            "android", "systemui", "packageinstaller", "permissioncontroller",
        ),
    )

    /**
     * Paket adı ve sistem bayrağına göre uygun kategoriyi döner.
     *
     * @param packageName Uygulamanın paket adı (örn. "com.whatsapp").
     * @param isSystem Uygulama sistem uygulaması mı?
     * @return Eşleşen [AppCategory]; hiçbiri eşleşmezse [AppCategory.Other].
     */
    fun categorize(packageName: String, isSystem: Boolean): AppCategory {
        val pkg = packageName.lowercase()
        for ((category, keys) in categoryCatalog) {
            if (category == AppCategory.System && isSystem) return AppCategory.System
            if (keys.any { pkg.contains(it) }) return category
        }
        return if (isSystem) AppCategory.System else AppCategory.Other
    }
}
