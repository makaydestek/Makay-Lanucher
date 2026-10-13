package com.makay.launcher.data

import com.makay.launcher.domain.AppCategory
import com.makay.launcher.domain.AppCategorizer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [AppCategorizer.categorize] fonksiyonu için birim testleri.
 *
 * Bu testler enum-map tabanlı kategorizasyon mantığını ve sistem uygulaması
 * tespitini doğrular.
 */
class AppCategorizerTest {

    @Test
    fun categorize_socialApp_returnsSocial() {
        assertEquals(AppCategory.Social, AppCategorizer.categorize("com.whatsapp", false))
        assertEquals(AppCategory.Social, AppCategorizer.categorize("org.telegram.messenger", false))
        assertEquals(AppCategory.Social, AppCategorizer.categorize("com.instagram.android", false))
        assertEquals(AppCategory.Social, AppCategorizer.categorize("com.discord", false))
    }

    @Test
    fun categorize_communicationApp_returnsCommunication() {
        assertEquals(AppCategory.Communication, AppCategorizer.categorize("com.google.android.dialer", false))
        assertEquals(AppCategory.Communication, AppCategorizer.categorize("com.google.android.gm", false))
        assertEquals(AppCategory.Communication, AppCategorizer.categorize("com.microsoft.outlook", false))
        assertEquals(AppCategory.Communication, AppCategorizer.categorize("us.zoom.videomeetings", false))
    }

    @Test
    fun categorize_mediaApp_returnsMedia() {
        assertEquals(AppCategory.Media, AppCategorizer.categorize("com.google.android.youtube", false))
        assertEquals(AppCategory.Media, AppCategorizer.categorize("com.spotify.music", false))
        assertEquals(AppCategory.Media, AppCategorizer.categorize("com.netflix.mediaclient", false))
        assertEquals(AppCategory.Media, AppCategorizer.categorize("org.videolan.vlc", false))
    }

    @Test
    fun categorize_gameApp_returnsGames() {
        assertEquals(AppCategory.Games, AppCategorizer.categorize("com.roblox.client", false))
        assertEquals(AppCategory.Games, AppCategorizer.categorize("com.mojang.minecraftpe", false))
        assertEquals(AppCategory.Games, AppCategorizer.categorize("com.supercell.clashofclans", false))
    }

    @Test
    fun categorize_productivityApp_returnsProductivity() {
        assertEquals(AppCategory.Productivity, AppCategorizer.categorize("com.google.android.apps.docs", false))
        assertEquals(AppCategory.Productivity, AppCategorizer.categorize("com.microsoft.office.word", false))
        assertEquals(AppCategory.Productivity, AppCategorizer.categorize("com.slack", false))
        assertEquals(AppCategory.Productivity, AppCategorizer.categorize("com.notion.id", false))
    }

    @Test
    fun categorize_toolApp_returnsTools() {
        assertEquals(AppCategory.Tools, AppCategorizer.categorize("com.android.settings", false))
        assertEquals(AppCategory.Tools, AppCategorizer.categorize("com.google.android.calculator", false))
        assertEquals(AppCategory.Tools, AppCategorizer.categorize("org.mozilla.firefox", false))
    }

    @Test
    fun categorize_systemFlag_returnsSystem() {
        // isSystem=true olan uygulama her zaman System döner
        assertEquals(AppCategory.System, AppCategorizer.categorize("com.unknown.package", true))
    }

    @Test
    fun categorize_systemPackageName_returnsSystem() {
        // Paket adından tespit
        assertEquals(AppCategory.System, AppCategorizer.categorize("com.android.systemui", false))
        assertEquals(AppCategory.System, AppCategorizer.categorize("com.android.packageinstaller", false))
    }

    @Test
    fun categorize_unknownApp_returnsOther() {
        assertEquals(AppCategory.Other, AppCategorizer.categorize("com.unknown.randomapp", false))
        assertEquals(AppCategory.Other, AppCategorizer.categorize("io.somedev.mytool", false))
    }
}
