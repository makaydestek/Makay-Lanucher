package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uygulama çökmelerini yerel dosyaya kaydeden basit hata yakalayıcı.
 *
 * - Çökme anında thread adı, cihaz modeli, Android versiyonu ve uygulama versiyonu kaydedilir.
 * - Log dosyası maksimum [MAX_LOG_BYTES] boyutuna ulaşınca eski kayıt arşivlenir.
 * - [shareIntent] ile kayıt e-posta/uygulama üzerinden paylaşılabilir.
 */
object CrashLogger {
    private const val FILE = "makay-crash.log"
    private const val ARCHIVE_FILE = "makay-crash.old.log"

    /** Log dosyası bu boyutu aşınca arşivlenir (512 KB). */
    private const val MAX_LOG_BYTES = 512 * 1024L

    /**
     * Uygulamanın global uncaught-exception handler'ını kurar.
     * Önceki handler zinciri korunur, bu sayede LeakCanary gibi araçlar etkilenmez.
     *
     * @param context Uygulama context'i (applicationContext kullanılır).
     */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /**
     * Son çökme kaydını döner. Kayıt yoksa boş string döner.
     *
     * @param context Uygulama context'i.
     * @return Log dosyasının içeriği.
     */
    fun lastCrash(context: Context): String =
        runCatching { context.filesDir.resolve(FILE).takeIf { it.exists() }?.readText().orEmpty() }
            .getOrDefault("")

    /**
     * Mevcut ve arşiv log dosyasını siler.
     *
     * @param context Uygulama context'i.
     */
    fun clear(context: Context) {
        runCatching {
            context.filesDir.resolve(FILE).delete()
            context.filesDir.resolve(ARCHIVE_FILE).delete()
        }
    }

    /**
     * Çökme kaydını paylaşmak için hazır bir [Intent] döner.
     * Kayıt yoksa null döner.
     *
     * @param context Uygulama context'i.
     * @return Paylaşım intent'i ya da null.
     */
    fun shareIntent(context: Context): Intent? {
        val text = lastCrash(context)
        if (text.isBlank()) return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Makay Launcher çökme kaydı")
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    /** Çökme verisini dosyaya yazar. Dosya büyüdüyse arşivler. */
    private fun write(context: Context, thread: Thread, error: Throwable) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()

        // Cihaz ve versiyon bilgisi – destek sırasında kritik
        val deviceInfo = buildString {
            appendLine("=== Makay Launcher Çökme Raporu ===")
            appendLine("Zaman     : $stamp")
            appendLine("Thread    : ${thread.name} (id=${thread.id})")
            appendLine("Cihaz     : ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android   : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            runCatching {
                val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                appendLine("Uygulama  : ${pi.versionName} (kod ${pi.longVersionCode})")
            }
            appendLine("ABI       : ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}")
            appendLine("====================================")
            appendLine()
            append(stack)
        }

        val logFile = context.filesDir.resolve(FILE)
        // Log dosyası büyüdüyse eskiyi arşivle, yeni dosyayla başla
        if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
            runCatching { logFile.copyTo(context.filesDir.resolve(ARCHIVE_FILE), overwrite = true) }
            logFile.delete()
        }
        logFile.appendText(deviceInfo + "\n")
    }
}
