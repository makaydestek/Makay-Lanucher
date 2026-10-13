package com.makay.launcher.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.R
import com.makay.launcher.BuildConfig
import com.makay.launcher.domain.AppEdition
import com.makay.launcher.domain.FormFactor
import com.makay.launcher.domain.GestureAction
import com.makay.launcher.domain.HomeStyle
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.IconShape
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var diagTick by remember { mutableIntStateOf(0) }
    var usageAccess by remember { mutableStateOf(false) }
    var notificationAccess by remember { mutableStateOf(false) }
    var crashLog by remember { mutableStateOf("") }
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

            Text(
            "Makay Launcher ${BuildConfig.VERSION_NAME} · Mobil / TV sürüm profilleri",
            color = Paper.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 32.dp),
        )
    }
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
