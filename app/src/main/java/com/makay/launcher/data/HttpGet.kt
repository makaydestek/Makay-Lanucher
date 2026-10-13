package com.makay.launcher.data

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Yönlendirme (redirect) destekli basit HTTP GET yardımcısı.
 *
 * Harici kütüphane kullanmadan `HttpURLConnection` üzerinden metin ve ikili içerik
 * indirir. HTTP → HTTPS gibi protokol değiştiren yönlendirmeler dahil olmak üzere
 * en fazla [MAX_REDIRECTS] adıma kadar yönlendirmeyi manuel olarak takip eder.
 *
 * Tüm ağ işlemleri çağıran coroutine'in dispatcher'ında yürütülür;
 * bu nesnenin çağrıları `Dispatchers.IO` bloğu içinden yapılmalıdır.
 */
object HttpGet {

    /** Güncelleme manifest dosyasının varsayılan URL'i. */
    const val MANIFEST_URL = "https://raw.githubusercontent.com/makaydestek/Makay-Lanucher/main/latest.json"

    /** Metin istekleri için varsayılan bağlantı + okuma zaman aşımı (ms). */
    private const val DEFAULT_TEXT_TIMEOUT_MS = 10_000

    /** Dosya indirme işlemleri için varsayılan bağlantı + okuma zaman aşımı (ms). */
    private const val DEFAULT_DOWNLOAD_TIMEOUT_MS = 30_000

    /** Tek bir istek için izin verilen maksimum yönlendirme adımı. */
    private const val MAX_REDIRECTS = 5

    /** Manifest ve hava durumu gibi metin yanıtları için üst sınır. */
    private const val MAX_TEXT_BYTES = 512 * 1024

    /** APK indirmesi için üst sınır. */
    private const val MAX_DOWNLOAD_BYTES = 128L * 1024 * 1024

    /**
     * [url] adresindeki kaynağı metin olarak döner.
     *
     * @param url Hedef URL.
     * @param timeoutMs Bağlantı ve okuma zaman aşımı (ms).
     * @return Sunucu yanıtının tüm içeriği.
     * @throws IOException Ağ hatası veya HTTP hata kodu durumunda.
     */
    fun text(url: String, timeoutMs: Int = DEFAULT_TEXT_TIMEOUT_MS): String {
        val (connection, input) = openWithRedirects(url, timeoutMs)
        return try {
            val bytes = readLimited(input, MAX_TEXT_BYTES.toLong())
            bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * [url] adresindeki kaynağı [dest] dosyasına indirir.
     *
     * @param url Hedef URL.
     * @param dest İçeriğin yazılacağı hedef dosya.
     * @param timeoutMs Bağlantı ve okuma zaman aşımı (ms).
     * @throws IOException Ağ hatası veya HTTP hata kodu durumunda.
     */
    fun download(url: String, dest: File, timeoutMs: Int = DEFAULT_DOWNLOAD_TIMEOUT_MS) {
        val (connection, input) = openWithRedirects(url, timeoutMs)
        try {
            input.use { inStream ->
                dest.outputStream().use { outStream -> copyLimited(inStream, outStream, MAX_DOWNLOAD_BYTES) }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * GitHub release gibi çok adımlı yönlendirme içeren URL'leri izler ve
     * nihai bağlantı + giriş akışı çiftini döner.
     *
     * @param initialUrl Başlangıç URL'i.
     * @param timeoutMs Bağlantı ve okuma zaman aşımı (ms).
     * @param maxRedirects İzin verilen maksimum yönlendirme adımı.
     * @return Açık [HttpURLConnection] ve bağlı [java.io.InputStream] çifti.
     * @throws IOException Zaman aşımı, HTTP hatası veya maksimum yönlendirme aşıldığında.
     */
    private fun openWithRedirects(
        initialUrl: String,
        timeoutMs: Int,
        maxRedirects: Int = MAX_REDIRECTS,
    ): Pair<HttpURLConnection, java.io.InputStream> {
        var currentUrl = initialUrl
        requireHttps(currentUrl)
        var redirects = 0
        while (redirects < maxRedirects) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "MakayLauncher")
                setRequestProperty("Accept", "*/*")
            }

            val status = connection.responseCode
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("HTTP $status: Yönlendirme adresi boş ($currentUrl)")
                }
                currentUrl = if (location.startsWith("https://")) {
                    location
                } else if (location.startsWith("http://")) {
                    throw IOException("HTTP yönlendirmesi reddedildi")
                } else {
                    URL(URL(currentUrl), location).toString()
                }
                requireHttps(currentUrl)
                redirects++
                continue
            }

            if (status !in 200..299) {
                connection.disconnect()
                throw IOException("HTTP sunucu hatası: $status ($currentUrl)")
            }

            return connection to connection.inputStream
        }
        throw IOException("Çok fazla yönlendirme — maksimum $maxRedirects adım aşıldı")
    }

    private fun requireHttps(url: String) {
        val protocol = runCatching { URL(url).protocol }.getOrElse {
            throw IOException("Geçersiz adres")
        }
        if (!protocol.equals("https", ignoreCase = true)) {
            throw IOException("Yalnızca https adresleri kabul edilir")
        }
    }

    private fun readLimited(input: InputStream, maxBytes: Long): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        copyLimited(input, buffer, maxBytes)
        return buffer.toByteArray()
    }

    private fun copyLimited(input: InputStream, output: java.io.OutputStream, maxBytes: Long) {
        val chunk = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("Yanıt boyutu sınırı aşıldı")
            output.write(chunk, 0, read)
        }
    }

    /**
     * Verilen [versionName] için GitHub release APK URL'ini döner.
     *
     * @param versionName Sürüm adı (örn. "1.0.9").
     * @return Tam APK indirme URL'i.
     */
    fun apkReleaseUrl(versionName: String): String =
        "https://github.com/makaydestek/Makay-Lanucher/releases/download/$versionName/MakayLauncher-$versionName.apk"
}
