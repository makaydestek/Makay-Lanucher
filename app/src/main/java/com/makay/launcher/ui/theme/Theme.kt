package com.makay.launcher.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.makay.launcher.data.WallpaperPalette

// ─── Renk Paleti ──────────────────────────────────────────────────────────────
/** Duvar kağıdı rengi ile uyumlu altın-yeşil vurgu tonu (HSL 42°, %55, %56). */
private val ColorGold = Color(0xFFC6A15B)

/** İkincil ton: soğuk pastel yeşil (HSL 165°, %30, %66). */
private val ColorSage = Color(0xFF8FBFB0)

/** Yüzey arka planı: koyu yarı-şeffaf yeşil-siyah (HSL 155°, %15, %9). */
private val ColorSurface = Color(0xCC121816)

/** Metin rengi: krem-kağıt beyazı. */
private val ColorOnSurface = Color(0xFFF4F1EA)

/**
 * Android 11 ve altı için sabit koyu renk şeması.
 * Renk değerleri HSL sistemiyle birbirleriyle uyumlu seçilmiştir.
 */
private val FallbackScheme = darkColorScheme(
    primary = ColorGold,
    onPrimary = Color(0xFF1B2A24),
    primaryContainer = Color(0xFF3A2E10),
    onPrimaryContainer = Color(0xFFFFE0A0),
    secondary = ColorSage,
    onSecondary = Color(0xFF102018),
    secondaryContainer = Color(0xFF1A3028),
    onSecondaryContainer = Color(0xFFB0E0D0),
    tertiary = Color(0xFF9BA4B4),
    background = Color.Transparent,
    surface = ColorSurface,
    onBackground = ColorOnSurface,
    onSurface = ColorOnSurface,
    surfaceVariant = Color(0xFF1E2A24),
    onSurfaceVariant = Color(0xFFB8C8C0),
    outline = Color(0x66FFFFFF),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF370000),
)

/**
 * Uygulama genelinde kullanılan Makay Launcher teması.
 *
 * Android 12+ (API 31) cihazlarda [dynamicColors] aktifse sistem duvar kağıdından
 * türetilen Material You renk şeması kullanılır; eski cihazlarda veya kullanıcı
 * dinamik rengi kapattığında [FallbackScheme] devreye girer.
 *
 * Glassmorphism efektleri için [Glass], [GlassStrong] ve [GlassStroke] sabitlerini kullanın.
 *
 * @param dynamicColors Dinamik Material You renk şeması kullanılsın mı?
 * @param content Temayı saran Composable içerik.
 */
@Composable
fun MakayTheme(
    dynamicColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val wallpaperTick = WallpaperPalette.generation.collectAsState().value
    val scheme = remember(dynamicColors, wallpaperTick) {
        if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context).copy(
                background = Color.Transparent,
                surface = ColorSurface,
            )
        } else {
            FallbackScheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = MakayTypography,
        content = content,
    )
}

/**
 * Makay Launcher tipografi sistemi.
 *
 * System-default FontFamily kullanılır; cihaz üreticisi özel fontları varsa
 * otomatik olarak uygulanır. Boyutlar Material 3 type-scale önerilerine göre
 * ayarlanmıştır.
 */
private val MakayTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ─── Glassmorphism Renk Sabitleri ─────────────────────────────────────────────

/** Şeffaf cam yüzey rengi — düşük kontrast arka planlar için. */
val Glass = Color(0x66101814)

/** Güçlü şeffaf cam yüzey rengi — kartlar ve panel arka planları için. */
val GlassStrong = Color(0x99101814)

/** Cam yüzey kenar çizgisi rengi — hafif beyaz ışıma efekti. */
val GlassStroke = Color(0x33FFFFFF)

/** Üzerine yazı veya ikon yerleştirilen kağıt tonlu yüzey rengi. */
val Paper = Color(0xFFF4F1EA)

