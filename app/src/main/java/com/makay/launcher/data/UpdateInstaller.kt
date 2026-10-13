package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Uygulama güncellemelerini indirip doğrulayan ve kurulum intent'i üreten yardımcı nesne.
 *
 * Tipik kullanım akışı:
 * 1. [download] → APK dosyasını önbelleğe indirir.
 * 2. [verify] → SHA-256 özeti ile bütünlüğü doğrular.
 * 3. [installIntent] → Sistem paketi yükleyicisini başlatacak [Intent] döner.
 */
object UpdateInstaller {

    /** İndirilen APK'nın geçerli sayılacağı minimum bayt boyutu (1 MB). */
    private const val MIN_APK_BYTES = 1_000_000L

    /**
     * APK'yı [apkUrl] adresinden indirir ve uygulama önbellekteki geçici dosyaya yazar.
     *
     * @param context Uygulama context'i (önbellek dizini için kullanılır).
     * @param apkUrl APK'nın tam URL'i.
     * @param onStatus İlerleme mesajlarını tüketen lambda.
     * @return İndirilen APK'nın [File] referansı.
     * @throws IllegalStateException APK boşsa veya minimum boyutun altındaysa.
     */
    fun download(context: Context, apkUrl: String, onStatus: (String) -> Unit = {}): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val dest = File(dir, "MakayLauncher-update.apk")
        if (dest.exists()) dest.delete()
        onStatus("İndiriliyor…")
        HttpGet.download(apkUrl, dest)
        check(dest.exists() && dest.length() >= MIN_APK_BYTES) {
            "APK geçersiz: beklenen en az ${MIN_APK_BYTES / 1024} KB, alınan ${dest.length() / 1024} KB"
        }
        return dest
    }

    /**
     * APK dosyasının SHA-256 özetini doğrular.
     *
     * [expectedSha256] boşsa sidecar dosyası (`.sha256`) üzerinden hash çekilmeye çalışılır.
     * Her iki kaynakta da hash yoksa kurulum reddedilir.
     *
     * @param apk İndirilmiş APK dosyası.
     * @param expectedSha256 Manifest'te bildirilen beklenen SHA-256 özeti.
     * @param apkUrl Sidecar kontrolü için APK'nın URL'i.
     * @param onStatus İlerleme mesajlarını tüketen lambda.
     * @throws IllegalStateException Hash uyuşmazlığı durumunda.
     */
    fun verify(apk: File, expectedSha256: String, apkUrl: String, onStatus: (String) -> Unit = {}) {
        onStatus("Dosya doğrulanıyor…")
        val expected = expectedSha256.ifBlank { sidecarHash(apkUrl) }
        if (expected.isBlank()) error("APK SHA-256 özeti yok — kurulum engellendi.")
        val actual = ApkHasher.sha256(apk)
        check(ApkHasher.matches(actual, expected)) {
            "APK SHA-256 özeti uyuşmuyor — indirme iptal edildi. (beklenen: ${expected.take(12)}…)"
        }
    }

    /**
     * Sistem paketi yükleyicisini başlatmak için gerekli [Intent]'i oluşturur.
     *
     * @param context Uygulama context'i.
     * @param apk Kurulacak APK dosyası.
     * @return Kurulum intent'i.
     */
    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.update", apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Sidecar `.sha256` dosyasını çekerek hash döner; hata durumunda boş string döner. */
    private fun sidecarHash(apkUrl: String): String {
        if (apkUrl.isBlank()) return ""
        val sidecar = runCatching { HttpGet.text("$apkUrl.sha256") }.getOrNull().orEmpty()
        return ApkHasher.parseSidecar(sidecar)
    }
}

