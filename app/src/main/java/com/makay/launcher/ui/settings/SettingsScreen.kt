package com.makay.launcher.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.makay.launcher.ui.components.DpadAction
import com.makay.launcher.ui.components.DpadChip
import com.makay.launcher.ui.components.dpadClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.R
import com.makay.launcher.BuildConfig
import android.net.Uri
import com.makay.launcher.domain.AppEdition
import com.makay.launcher.domain.FormFactor
import com.makay.launcher.domain.GestureAction
import com.makay.launcher.domain.HomeStyle
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.IconShape
import com.makay.launcher.domain.Overlay
import com.makay.launcher.domain.TvCardIconMode
import com.makay.launcher.domain.TvCardSize
import com.makay.launcher.domain.TvFamilySlot
import com.makay.launcher.domain.TvFocusEffect
import com.makay.launcher.domain.TvIdleAction
import com.makay.launcher.domain.TvWallpaperKind
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper

@Composable
fun SettingsScreen(state: HomeUiState, viewModel: HomeViewModel) {
    val settings = state.settings
    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.updateSettings { it.copy(searchContacts = granted) }
    }
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshGlance() }
    val exportBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importBackup) }
    val pickTvWallpaper = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.applyTvWallpaperUri(uri)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var diagTick by remember { mutableIntStateOf(0) }
    var usageAccess by remember { mutableStateOf(false) }
    var notificationAccess by remember { mutableStateOf(false) }
    var crashLog by remember { mutableStateOf("") }
    var parentalPinDraft by remember { mutableStateOf("") }
    LaunchedEffect(lifecycle, diagTick) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            usageAccess = withContext(Dispatchers.IO) { viewModel.hasUsageAccess() }
            notificationAccess = withContext(Dispatchers.IO) { viewModel.isNotificationAccessGranted() }
            crashLog = withContext(Dispatchers.IO) { viewModel.lastCrashLog() }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassStrong)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_title), color = Paper, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            DpadAction(stringResource(R.string.close), onClick = viewModel::closeOverlays)
        }

        val profile = state.profile
        Section(stringResource(R.string.section_edition)) {
            val hardware = when (state.formFactor) {
                FormFactor.PHONE -> "Telefon"
                FormFactor.TABLET -> "Tablet"
                FormFactor.TV -> "Android TV"
            }
            Text(
                if (profile.isTv) "Aktif: TV Sürümü" else "Aktif: Mobil / Tablet Sürümü",
                color = Paper,
            )
            Text("Donanım: $hardware", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                DpadChip(
                    label = "Mobil / Tablet",
                    selected = settings.edition == AppEdition.MOBILE,
                    onClick = { viewModel.selectEdition(AppEdition.MOBILE) },
                )
                DpadChip(
                    label = "TV",
                    selected = settings.edition == AppEdition.TV,
                    onClick = { viewModel.selectEdition(AppEdition.TV) },
                )
            }
            Text(
                if (profile.isTv) {
                    "TV’de varsayılan ana ekran: Ayarlar → Ana ekran / Başlatıcı → Makay Launcher. Kumanda ile satırları seç, OK ile onayla."
                } else {
                    "Seçim kalıcıdır. Kullanılmayan sürümün servisleri çalışmaz."
                },
                color = Paper.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (profile.mobileHomeStyles) Section(stringResource(R.string.section_home_style)) {
            DpadChip("Izgara · Nova / Lawnchair", settings.homeStyle == HomeStyle.GRID) {
                viewModel.updateSettings { it.copy(homeStyle = HomeStyle.GRID) }
            }
            DpadChip("Liste · Niagara", settings.homeStyle == HomeStyle.NIAGARA) {
                viewModel.updateSettings { it.copy(homeStyle = HomeStyle.NIAGARA) }
            }
            DpadChip("Kutucuk · Square Home", settings.homeStyle == HomeStyle.TILES) {
                viewModel.updateSettings { it.copy(homeStyle = HomeStyle.TILES) }
            }
            DpadChip("Panel · AIO", settings.homeStyle == HomeStyle.DASHBOARD) {
                viewModel.updateSettings { it.copy(homeStyle = HomeStyle.DASHBOARD) }
            }
            DpadChip("Metin · Olauncher", settings.homeStyle == HomeStyle.TEXT) {
                viewModel.updateSettings { it.copy(homeStyle = HomeStyle.TEXT) }
            }
        }

        if (profile.gestures) Section(stringResource(R.string.section_gestures)) {
            GestureRow("Yukarı kaydır", settings.swipeUp) { action ->
                viewModel.updateSettings { it.copy(swipeUp = action) }
            }
            GestureRow("Aşağı kaydır", settings.swipeDown) { action ->
                viewModel.updateSettings { it.copy(swipeDown = action) }
            }
            GestureRow("Çift dokun", settings.doubleTap) { action ->
                viewModel.updateSettings { it.copy(doubleTap = action) }
            }
            Text("Uzun bas: ayarlar", color = Paper.copy(alpha = 0.6f), fontSize = 12.sp)
        }

        Section(stringResource(R.string.section_common)) {
            Toggle("Material You dinamik renk", settings.dynamicColors) {
                viewModel.updateSettings { s -> s.copy(dynamicColors = it) }
            }
            if (profile.glanceSmart) Toggle("At a Glance (Lawnchair)", settings.showAtAGlance) {
                viewModel.updateSettings { s -> s.copy(showAtAGlance = it) }
            }
            if (profile.dock) Toggle("Dock çubuğu", settings.showDock) {
                viewModel.updateSettings { s -> s.copy(showDock = it) }
            }
            if (profile.mobileHomeStyles) Toggle("Alfabe rayı (Niagara)", settings.showAlphabetRail) {
                viewModel.updateSettings { s -> s.copy(showAlphabetRail = it) }
            }
            Toggle("Otomatik kategoriler (Smart)", settings.autoCategories) {
                viewModel.updateSettings { s -> s.copy(autoCategories = it) }
            }
            if (profile.mobileHomeStyles) Toggle("Yalnızca metin (Olauncher)", settings.textOnly) {
                viewModel.updateSettings { s -> s.copy(textOnly = it) }
            }
            if (profile.mobileHomeStyles) Toggle("Dijital detoks", settings.digitalDetox) {
                viewModel.updateSettings { s -> s.copy(digitalDetox = it) }
            }
            if (profile.contactsSearch) Toggle("Kişilerde ara (Kvaesitso)", settings.searchContacts) { enabled ->
                if (enabled) contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                else viewModel.updateSettings { s -> s.copy(searchContacts = false) }
            }
        }

        if (profile.widgets) Section(stringResource(R.string.section_widgets)) {
            DpadAction("Üçüncü parti widget ekle", onClick = viewModel::openWidgetPicker)
            Text(
                if (state.widgets.isEmpty()) "Ana ekranda widget yok"
                else "${state.widgets.size} widget sabitlendi · uzun basarak kaldırın",
                color = Paper.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }

        if (profile.iconPacks) Section(stringResource(R.string.section_icon_packs)) {
            DpadChip("Sistem ikonları", settings.iconPackPackage.isBlank()) {
                viewModel.applyIconPack("")
            }
            if (state.iconPacks.isEmpty()) {
                Text(
                    "Yüklü icon pack bulunamadı. Nova / ADW / GO uyumlu bir paket kurun.",
                    color = Paper.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                state.iconPacks.forEach { pack ->
                    DpadChip(pack.label, settings.iconPackPackage == pack.packageName) {
                        viewModel.applyIconPack(pack.packageName)
                    }
                }
            }
        }

        Section(stringResource(R.string.section_launcher)) {
            Text(
                if (state.isDefaultLauncher) {
                    "Makay şu an varsayılan ana ekran"
                } else {
                    "Makay henüz varsayılan değil"
                },
                color = Paper,
            )
            Text(
                "Kurulum normal uygulamadır. Varsayılan yapmak için aşağıdaki satıra dokunun; sistem listesinden Makay’ı seçin.",
                color = Paper.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            DpadAction(
                if (state.isDefaultLauncher) "Önceki / sistem launcher’ına dön" else "Varsayılan launcher yap",
                emphasis = !state.isDefaultLauncher,
                onClick = viewModel::openDefaultHomePicker,
            )
            if (profile.isTv) {
                DpadAction("Ekran koruyucu (screensaver)", onClick = viewModel::openDreamSettings)
            }
        }

        if (profile.isTv) {
            Section("TV · Satırlar") {
                DpadAction("Satır düzenleyici") { viewModel.open(Overlay.TvRows) }
                Toggle("Odak üstü satırları gizle", settings.tvHideRowsAboveFocus) {
                    viewModel.updateSettings { s -> s.copy(tvHideRowsAboveFocus = it) }
                }
                Toggle("Önizleme kanalları (Watch Next)", settings.tvChannelsEnabled) {
                    viewModel.updateSettings { s -> s.copy(tvChannelsEnabled = it) }
                }
            }

            Section("TV · Kart görünümü") {
                Text("Boyut", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvCardSize.entries.forEach { size ->
                        DpadChip(
                            when (size) {
                                TvCardSize.SMALL -> "Küçük"
                                TvCardSize.MEDIUM -> "Orta"
                                TvCardSize.LARGE -> "Büyük"
                            },
                            settings.tvCardSize == size,
                        ) { viewModel.updateSettings { it.copy(tvCardSize = size) } }
                    }
                }
                Text("Odak efekti", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvFocusEffect.entries.forEach { effect ->
                        DpadChip(
                            when (effect) {
                                TvFocusEffect.SCALE -> "Ölçek"
                                TvFocusEffect.GLOW -> "Parıltı"
                                TvFocusEffect.SHADOW -> "Gölge"
                                TvFocusEffect.NONE -> "Yok"
                            },
                            settings.tvFocusEffect == effect,
                        ) { viewModel.updateSettings { it.copy(tvFocusEffect = effect) } }
                    }
                }
                Text("Köşe / saydamlık", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(8, 14, 18, 24).forEach { corner ->
                        DpadChip("${corner}dp", settings.tvCardCorner == corner) {
                            viewModel.updateSettings { it.copy(tvCardCorner = corner) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf(0.55f, 0.7f, 0.85f, 1f).forEach { opacity ->
                        DpadChip("${(opacity * 100).toInt()}%", settings.tvCardOpacity == opacity) {
                            viewModel.updateSettings { it.copy(tvCardOpacity = opacity) }
                        }
                    }
                }
            }

            Section("TV · Duvar kağıdı") {
                Text(
                    when (settings.tvWallpaperKind) {
                        TvWallpaperKind.NONE -> "Özel wallpaper kapalı (sistem)"
                        else -> "${settings.tvWallpaperKind.name} seçili"
                    },
                    color = Paper.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                )
                DpadAction("Görsel / GIF seç") { pickTvWallpaper.launch(arrayOf("image/*")) }
                DpadAction("Video seç") { pickTvWallpaper.launch(arrayOf("video/*")) }
                DpadAction("Özel wallpaper temizle", onClick = viewModel::clearTvWallpaper)
                Text("Karartma", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.15f, 0.35f, 0.55f, 0.75f).forEach { dim ->
                        DpadChip("${(dim * 100).toInt()}%", settings.tvWallpaperDim == dim) {
                            viewModel.updateSettings { it.copy(tvWallpaperDim = dim) }
                        }
                    }
                }
                Text("Parlaklık / bulanıklık", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5f, 0.75f, 1f).forEach { bright ->
                        DpadChip("P${(bright * 100).toInt()}", settings.tvWallpaperBrightness == bright) {
                            viewModel.updateSettings { it.copy(tvWallpaperBrightness = bright) }
                        }
                    }
                    listOf(0, 8, 16, 24).forEach { blur ->
                        DpadChip("B$blur", settings.tvWallpaperBlur == blur) {
                            viewModel.updateSettings { it.copy(tvWallpaperBlur = blur) }
                        }
                    }
                }
            }

            Section("TV · Açılış / boşta") {
                Text(
                    if (settings.tvAutostartKey.isBlank()) "Autostart kapalı"
                    else "Autostart: ${settings.tvAutostartKey}",
                    color = Paper.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                )
                DpadAction("Autostart: ilk favori") {
                    viewModel.setAutostartApp(state.favorites.firstOrNull())
                }
                DpadAction("Autostart: ilk HDMI") {
                    viewModel.setAutostartInput(state.inputSources.firstOrNull())
                }
                DpadAction("Autostart kapat") { viewModel.setAutostartApp(null) }
                Text("Boşta kalınca", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvIdleAction.entries.forEach { action ->
                        DpadChip(
                            when (action) {
                                TvIdleAction.NONE -> "Yok"
                                TvIdleAction.SCREENSAVER -> "Koruyucu"
                                TvIdleAction.DIM -> "Karart"
                                TvIdleAction.STANDBY -> "Bekleme"
                            },
                            settings.tvIdleAction == action,
                        ) { viewModel.updateSettings { it.copy(tvIdleAction = action) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf(2, 5, 10, 15).forEach { min ->
                        DpadChip("${min}dk", settings.tvIdleMinutes == min) {
                            viewModel.updateSettings { it.copy(tvIdleMinutes = min) }
                        }
                    }
                }
            }

            Section("TV · Ebeveyn kontrolü") {
                Toggle("Ebeveyn kilidi", settings.tvParentalEnabled) {
                    viewModel.updateSettings { s -> s.copy(tvParentalEnabled = it) }
                }
                Toggle("Zaman aralığı kullan", settings.tvParentalSchedule) {
                    viewModel.updateSettings { s -> s.copy(tvParentalSchedule = it) }
                }
                Text(
                    "Aktif pencere: ${settings.tvParentalStartMin / 60}:${"%02d".format(settings.tvParentalStartMin % 60)} – ${settings.tvParentalEndMin / 60}:${"%02d".format(settings.tvParentalEndMin % 60)}",
                    color = Paper.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DpadChip("21:00–07:00", selected = settings.tvParentalStartMin == 21 * 60) {
                        viewModel.updateSettings { it.copy(tvParentalStartMin = 21 * 60, tvParentalEndMin = 7 * 60) }
                    }
                    DpadChip("Tüm gün", selected = !settings.tvParentalSchedule) {
                        viewModel.updateSettings { it.copy(tvParentalSchedule = false) }
                    }
                }
                Text("PIN (4–8 rakam)", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = parentalPinDraft,
                    onValueChange = { parentalPinDraft = it.filter { ch -> ch.isDigit() }.take(8) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Paper, fontSize = 16.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
                DpadAction("PIN kaydet") {
                    viewModel.setParentalPin(parentalPinDraft)
                    parentalPinDraft = ""
                }
                Text("Favorileri engelle (dokunarak)", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.favorites.take(8).forEach { appInfo ->
                        val blocked = settings.tvBlockedPackages.split(',').map { it.trim() }.contains(appInfo.packageName)
                        DpadChip(appInfo.label.take(10), selected = blocked) {
                            viewModel.toggleBlockedPackage(appInfo.packageName)
                        }
                    }
                }
            }

            Section("TV · Kalibrasyon / mühendislik") {
                DpadAction("Kalibrasyon desenleri", onClick = viewModel::openCalibration)
                if (state.engineeringMenus.isEmpty()) {
                    Text(
                        "Cihazda bilinen mühendislik menüsü bulunamadı",
                        color = Paper.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                    )
                    DpadAction("Yeniden tara", onClick = viewModel::refreshEngineeringMenus)
                } else {
                    state.engineeringMenus.forEach { label ->
                        DpadAction(label) { viewModel.openEngineeringMenu(label) }
                    }
                }
            }

            Section("TV · Gelişmiş") {
                Toggle("Odak hatırla", settings.tvRememberFocus) {
                    viewModel.updateSettings { s -> s.copy(tvRememberFocus = it) }
                }
                Toggle("Burn-in soldurma", settings.tvBurnInFade) {
                    viewModel.updateSettings { s -> s.copy(tvBurnInFade = it) }
                }
                Toggle("Saati göster", settings.tvShowClock) {
                    viewModel.updateSettings { s -> s.copy(tvShowClock = it) }
                }
                Toggle("Ayarlar kartını gizle", settings.tvHideSettingsChip) {
                    viewModel.updateSettings { s -> s.copy(tvHideSettingsChip = it) }
                }
                Toggle("Video/önizleme odak", settings.tvVideoPreviewOnFocus) {
                    viewModel.updateSettings { s -> s.copy(tvVideoPreviewOnFocus = it) }
                }
                Toggle("İlerleme çubuğu", settings.tvWatchProgressBar) {
                    viewModel.updateSettings { s -> s.copy(tvWatchProgressBar = it) }
                }
                Toggle("Açılışta ekran koruyucu", settings.tvBootToScreensaver) {
                    viewModel.updateSettings { s -> s.copy(tvBootToScreensaver = it) }
                }
                Toggle("Misafir kilidi", settings.tvGuestLock) {
                    viewModel.updateSettings { s -> s.copy(tvGuestLock = it) }
                }
                Toggle("Erişilebilirlik yardımı", settings.tvAccessibilityAssist) {
                    viewModel.updateSettings { s -> s.copy(tvAccessibilityAssist = it) }
                }
                DpadAction("Erişilebilirlik ayarlarını aç", onClick = viewModel::openAccessibilitySettings)
                Toggle("Otomatik yedek", settings.tvAutoBackup) {
                    viewModel.updateSettings { s -> s.copy(tvAutoBackup = it) }
                }
                DpadAction("Otomatik yedekler") {
                    viewModel.refreshAutoBackupList()
                    viewModel.open(Overlay.AutoBackups)
                }
                DpadAction("URL’den APK kur") { viewModel.open(Overlay.Sideload) }
                Text("Kart aralığı", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(8, 14, 20, 28).forEach { sp ->
                        DpadChip("${sp}dp", settings.tvCardSpacing == sp) {
                            viewModel.updateSettings { it.copy(tvCardSpacing = sp) }
                        }
                    }
                }
                Text("Varsayılan ikon/banner", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DpadChip("İkon", settings.tvCardIconMode == TvCardIconMode.ICON) {
                        viewModel.updateSettings { it.copy(tvCardIconMode = TvCardIconMode.ICON) }
                    }
                    DpadChip("Banner", settings.tvCardIconMode == TvCardIconMode.BANNER) {
                        viewModel.updateSettings { it.copy(tvCardIconMode = TvCardIconMode.BANNER) }
                    }
                }
                Text("Video zoom", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.9f, 1f, 1.1f, 1.25f).forEach { z ->
                        DpadChip("${z}x", settings.tvVideoZoom == z) {
                            viewModel.updateSettings { it.copy(tvVideoZoom = z) }
                        }
                    }
                }
                Text("Wallpaper sağlayıcı", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DpadChip("Bing", settings.tvWallpaperKind == TvWallpaperKind.BING) {
                        viewModel.applyWallpaperProvider(TvWallpaperKind.BING)
                    }
                    DpadChip("Odak kartı", settings.tvWallpaperKind == TvWallpaperKind.FOCUSED) {
                        viewModel.applyWallpaperProvider(TvWallpaperKind.FOCUSED)
                    }
                }
                Text("Kontrast / doygunluk / hue", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.8f, 1f, 1.2f).forEach { v ->
                        DpadChip("K$v", settings.tvWallpaperContrast == v) {
                            viewModel.updateSettings { it.copy(tvWallpaperContrast = v) }
                        }
                    }
                    listOf(0.7f, 1f, 1.3f).forEach { v ->
                        DpadChip("D$v", settings.tvWallpaperSaturation == v) {
                            viewModel.updateSettings { it.copy(tvWallpaperSaturation = v) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf(-30f, 0f, 30f).forEach { h ->
                        DpadChip("H${h.toInt()}", settings.tvWallpaperHue == h) {
                            viewModel.updateSettings { it.copy(tvWallpaperHue = h) }
                        }
                    }
                }
                Text("Aile programı", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvFamilySlot.entries.forEach { slot ->
                        val on = settings.tvFamilySlots.split(',').map { it.trim() }.contains(slot.name)
                        DpadChip(slot.title, selected = on) { viewModel.toggleFamilySlot(slot) }
                    }
                }
                Text("HDMI engelle", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.inputSources.take(6).forEach { src ->
                        val blocked = settings.tvBlockedInputs.split(',').map { it.trim() }.contains(src.id)
                        DpadChip(src.label.take(12), selected = blocked) {
                            viewModel.toggleBlockedInput(src.id)
                        }
                    }
                }
                Text("Dil", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DpadChip("Sistem", settings.tvLocaleTag.isBlank()) { viewModel.setLocaleTag("") }
                    DpadChip("TR", settings.tvLocaleTag == "tr") { viewModel.setLocaleTag("tr") }
                    DpadChip("EN", settings.tvLocaleTag == "en") { viewModel.setLocaleTag("en") }
                }
                Text(
                    "ADB: adb shell am start -a com.makay.launcher.IMPORT_SETTINGS -d content://…",
                    color = Paper.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (state.inputSources.isNotEmpty()) {
                    DpadAction("Aktif HDMI için profil kaydet") {
                        viewModel.saveDisplayProfileForInput(state.inputSources.first().id)
                    }
                }
            }
        }

        Section(stringResource(R.string.section_usage_badges)) {
            Text(
                if (usageAccess) "Kullanım erişimi açık · sık kullanılanlar sistem verisiyle sıralanır"
                else "Kullanım erişimi kapalı · yalnızca Makay içinden açılanlar sayılır",
                color = Paper.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
            DpadAction("Kullanım erişimini aç", onClick = viewModel::openUsageAccess)
            if (profile.notificationBadges) {
                Text(
                    if (notificationAccess) "Bildirim rozetleri açık"
                    else "Bildirim rozetleri kapalı",
                    color = Paper.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Section(stringResource(R.string.section_updates)) {
            Text(
                "Açık: ${com.makay.launcher.BuildConfig.VERSION_NAME}",
                color = Paper,
            )
            Text(
                "Geliştirirken yeni APK’yı üzerine kurun. Adres doluysa açılışta Güncelle / Ertele çıkar.",
                color = Paper.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
            androidx.compose.foundation.text.BasicTextField(
                value = settings.updateUrl,
                onValueChange = { value -> viewModel.updateSettings { it.copy(updateUrl = value.trim()) } },
                textStyle = androidx.compose.ui.text.TextStyle(color = Paper, fontSize = 14.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                decorationBox = { inner ->
                    if (settings.updateUrl.isEmpty()) {
                        Text(
                            "https://raw.githubusercontent.com/makaydestek/Makay-Lanucher/main/latest.json",
                            color = Paper.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                        )
                    }
                    inner()
                },
            )
            DpadAction("Güncellemeleri denetle") { viewModel.checkForUpdate(force = true) }
            if (state.pendingUpdate != null) {
                DpadAction("Bekleyen sürüm: ${state.pendingUpdate.versionName}", onClick = viewModel::showPendingUpdate)
            }
        }

        Section(stringResource(R.string.section_quick_actions)) {
            if (profile.notificationBadges) {
                DpadAction("Bildirim rozetleri izni (Niagara)", onClick = viewModel::openNotificationAccess)
            }
            if (profile.bingWallpaper) {
                DpadAction("Sistem duvar kağıdı seçici", onClick = viewModel::openWallpaperPicker)
                DpadAction("Bing günlük duvar kağıdı (Microsoft)", onClick = viewModel::applyBingWallpaper)
            }
            if (profile.calendarGlance) {
                DpadAction("Takvim izni (At a Glance etkinlikleri)") {
                    calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                }
            }
        }

        Section(stringResource(R.string.section_backup)) {
            DpadAction("Düzeni JSON olarak dışa aktar") { exportBackup.launch("makay-launcher-backup.json") }
            DpadAction("Yedekten geri yükle") { importBackup.launch(arrayOf("application/json", "text/plain")) }
            Text(
                "JSON yedek ayar, ızgara, kasa ve kilit listesini içerir. Widget’lar geri yüklemede yeniden bağlanır.",
                color = Paper.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }

        if (profile.workspaceGrid) Section(stringResource(R.string.section_grid)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 5, 6).forEach { count ->
                    DpadChip("$count sütun", settings.gridColumns == count) { viewModel.setGrid(columns = count) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf(4, 5, 6).forEach { count ->
                    DpadChip("$count satır", settings.gridRows == count) { viewModel.setGrid(rows = count) }
                }
            }
        }

        Section("İkon maskesi") {
            IconShape.entries.forEach { shape ->
                DpadChip(
                    label = when (shape) {
                        IconShape.CIRCLE -> "Yuvarlak"
                        IconShape.SQUIRCLE -> "Squircle"
                        IconShape.TEARDROP -> "Damla"
                        IconShape.ROUNDED -> "Köşeli"
                    },
                    selected = settings.iconShape == shape,
                ) {
                    viewModel.applyIconPack(settings.iconPackPackage)
                    viewModel.updateSettings { it.copy(iconShape = shape) }
                }
            }
        }

        Section("Tanılama") {
            Text(
                if (crashLog.isBlank()) "Kayıtlı çökme yok" else crashLog.take(400),
                color = Paper.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            if (crashLog.isNotBlank()) {
                DpadAction("Çökme kaydını paylaş", onClick = viewModel::shareCrashLog)
                DpadAction("Çökme kaydını sil") {
                    viewModel.clearCrashLog()
                    diagTick++
                }
            }
        }

        AboutSection()
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Ayarlar → Hakkında alanı.
 *
 * Makay ekosistemindeki tüm uygulamalarda aynı sıra korunur:
 * ikonlu başlık, uygulama adı + sürüm, geliştirici ve telif satırı.
 */
@Composable
private fun AboutSection() {
    Spacer(Modifier.height(16.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.section_about),
            color = Paper,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                stringResource(R.string.app_name),
                color = Paper,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                color = Paper.copy(alpha = 0.65f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            AboutDivider()
            Text(
                stringResource(R.string.about_developer_title),
                color = Paper,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.about_developer_body),
                color = Paper.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            AboutDivider()
            Text(
                stringResource(R.string.about_copyright),
                color = Paper.copy(alpha = 0.55f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = Paper.copy(alpha = 0.12f),
        thickness = 1.dp,
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, color = Paper, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .dpadClickable { onChange(!checked) }
            .background(
                if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Paper, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun GestureRow(label: String, current: GestureAction, onPick: (GestureAction) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, color = Paper.copy(alpha = 0.7f), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GestureAction.entries.forEach { action ->
                DpadChip(action.label(), action == current) { onPick(action) }
            }
        }
    }
}

private fun GestureAction.label(): String = when (this) {
    GestureAction.None -> "Yok"
    GestureAction.Drawer -> "Çekmece"
    GestureAction.Search -> "Arama"
    GestureAction.ControlCenter -> "Kontrol"
    GestureAction.Notifications -> "Bildirim"
    GestureAction.Settings -> "Ayarlar"
}
