package com.makay.launcher.data

import com.makay.launcher.domain.AppCategorizer
import com.makay.launcher.domain.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * HttpGet, UpdateChecker ve ApkHasher için genişletilmiş birim testleri.
 *
 * Mevcut [LauncherLogicTest] testlerinin yanında çalışır ve özellikle
 * URL üretimi, sidecar parse ve kategorizasyon senaryolarını genişletir.
 */
class HttpGetExtendedTest {

    @Test
    fun apkReleaseUrl_hasCorrectFormat() {
        val url = HttpGet.apkReleaseUrl("2.0.0")
        assertEquals(
            "https://github.com/makaydestek/Makay-Lanucher/releases/download/2.0.0/MakayLauncher-2.0.0.apk",
            url,
        )
    }

    @Test
    fun apkHasher_emptyFileShouldHave64CharHash() {
        val file = File.createTempFile("test_empty", ".apk")
        file.writeBytes(byteArrayOf())
        val hash = ApkHasher.sha256(file)
        assertEquals("Boş dosya SHA-256 64 karakter olmalı", 64, hash.length)
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash,
        )
        file.delete()
    }

    @Test
    fun apkHasher_sameContentShouldMatchEachOther() {
        val f1 = File.createTempFile("apk-a", ".bin")
        val f2 = File.createTempFile("apk-b", ".bin")
        val content = byteArrayOf(1, 2, 3, 4, 5)
        f1.writeBytes(content)
        f2.writeBytes(content)
        val h1 = ApkHasher.sha256(f1)
        val h2 = ApkHasher.sha256(f2)
        assertEquals("Aynı içerik aynı hash üretmeli", h1, h2)
        assertTrue(ApkHasher.matches(h1, h2))
        f1.delete()
        f2.delete()
    }

    @Test
    fun apkHasher_differentContentShouldNotMatch() {
        val f1 = File.createTempFile("apk-c", ".bin").apply { writeBytes(byteArrayOf(1)) }
        val f2 = File.createTempFile("apk-d", ".bin").apply { writeBytes(byteArrayOf(2)) }
        assertFalse(ApkHasher.matches(ApkHasher.sha256(f1), ApkHasher.sha256(f2)))
        f1.delete()
        f2.delete()
    }

    @Test
    fun apkHasher_parseSidecar_ignoresComments() {
        val sidecar = """
            # Bu bir yorumdur
            ABC123  file.apk
        """.trimIndent()
        assertEquals("abc123", ApkHasher.parseSidecar(sidecar))
    }

    @Test
    fun updateChecker_parsesAllFields() {
        val json = """{"versionName":"2.1.0","versionCode":21,"apkUrl":"https://example.com/app.apk","notes":"Yeni özellikler","sha256":"ABCDEF"}"""
        val offer = UpdateChecker.parse(json)
        assertEquals("2.1.0", offer?.versionName)
        assertEquals(21, offer?.versionCode)
        assertEquals("https://example.com/app.apk", offer?.apkUrl)
        assertEquals("Yeni özellikler", offer?.notes)
        assertEquals("abcdef", offer?.sha256) // lowercase normalize
    }

    @Test
    fun updateChecker_rejectsZeroVersionCode() {
        assertNull(UpdateChecker.parse("""{"versionName":"1.0","versionCode":0}"""))
    }

    @Test
    fun updateChecker_rejectsBlankVersionName() {
        assertNull(UpdateChecker.parse("""{"versionName":"","versionCode":5}"""))
    }

    @Test
    fun httpGet_rejectsCleartext() {
        try {
            HttpGet.text("http://example.com/latest.json")
            throw AssertionError("http adresi reddedilmeli")
        } catch (error: IOException) {
            assertTrue(error.message.orEmpty().contains("https"))
        }
    }

    @Test
    fun updateInstaller_rejectsMissingHash() {
        val apk = File.createTempFile("update", ".apk")
        try {
            UpdateInstaller.verify(apk, "", "")
            throw AssertionError("Hash yokken kurulum engellenmeli")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("SHA-256"))
        } finally {
            apk.delete()
        }
    }
}
