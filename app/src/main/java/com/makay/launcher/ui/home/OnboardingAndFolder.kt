package com.makay.launcher.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.data.FormFactorDetector
import com.makay.launcher.domain.AppEdition
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.WorkspaceType
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.components.dpadClickable
import com.makay.launcher.ui.theme.Paper
import kotlinx.coroutines.delay

@Composable
fun OnboardingOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    val context = LocalContext.current
    val suggested = FormFactorDetector.suggestedEdition(context)
    val startFocus = remember { FocusRequester() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshGlance()
    }
    LaunchedEffect(suggested, state.profile.isChosen) {
        if (!state.profile.isChosen && suggested == AppEdition.TV) {
            viewModel.selectEdition(AppEdition.TV)
        }
    }
    LaunchedEffect(state.profile.isChosen, suggested) {
        delay(80)
        runCatching { startFocus.requestFocus() }
    }
    OverlayScrim(onClose = { }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                Column {
                    Text("Makay Launcher kurulumu", color = Paper, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Kumanda ile bir kartı seçin, sonra OK / ortadaki tuşa basın.",
                        color = Paper.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            EditionPickCard(
                title = "Mobil / Tablet Sürümü",
                detail = "Dokunma, jestler, dock, widget, klasör, kasa ve At a Glance.",
                selected = state.settings.edition == AppEdition.MOBILE,
                suggested = suggested == AppEdition.MOBILE,
                focusRequester = if (!state.profile.isChosen && suggested == AppEdition.MOBILE) startFocus else null,
                onClick = { viewModel.selectEdition(AppEdition.MOBILE) },
            )
            Spacer(Modifier.height(12.dp))
            EditionPickCard(
                title = "TV Sürümü",
                detail = "Kumanda / D-pad satırları, HDMI girişleri ve Leanback odak. Mobil özellikler kapalı.",
                selected = state.settings.edition == AppEdition.TV,
                suggested = suggested == AppEdition.TV,
                focusRequester = if (!state.profile.isChosen && suggested == AppEdition.TV) startFocus else null,
                onClick = { viewModel.selectEdition(AppEdition.TV) },
            )
            if (state.profile.isChosen) {
                Spacer(Modifier.height(16.dp))
                GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                    Column {
                        Text(
                            if (state.profile.isTv) "TV kurulumu" else "Mobil / tablet kurulumu",
                            color = Paper,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "İstediğiniz izinleri sonra Ayarlar’dan da verebilirsiniz. Takvim ve kişiler bakış kartı ile arama için; konum hava durumu için.",
                            color = Paper.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.isDefaultLauncher) {
                                "Varsayılan ana ekran: açık"
                            } else {
                                "Varsayılan ana ekran: kapalı — aşağıdaki satıra basıp Makay’ı seçin"
                            },
                            color = if (state.isDefaultLauncher) MaterialTheme.colorScheme.primary else Paper.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                        )
                        if (state.profile.isTv) {
                            Text(
                                "TV: Ayarlar → Cihaz tercihleri → Ana ekran / Başlatıcı → Makay Launcher. Kumanda ile satırı seçip OK basın.",
                                color = Paper.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        DpadActionRow("Varsayılan ana ekran yap") {
                            viewModel.openDefaultHomePicker()
                        }
                        if (state.profile.isMobile) {
                            DpadActionRow("Kullanım erişimi (sık kullanılanlar)") {
                                viewModel.openUsageAccess()
                            }
                        }
                        if (state.profile.isTv) {
                            DpadActionRow("Ekran koruyucu") {
                                viewModel.openDreamSettings()
                            }
                        }
                        if (state.profile.notificationBadges) {
                            DpadActionRow("Bildirim rozetleri") {
                                viewModel.openNotificationAccess()
                            }
                        }
                        if (state.profile.calendarGlance || state.profile.contactsSearch || state.profile.weather) {
                            DpadActionRow(
                                buildList {
                                    if (state.profile.calendarGlance) add("takvim")
                                    if (state.profile.contactsSearch) add("kişiler")
                                    if (state.profile.weather) add("konum")
                                }.joinToString(", ").replaceFirstChar { it.uppercase() },
                            ) {
                                val extras = buildList {
                                    if (state.profile.calendarGlance) add(Manifest.permission.READ_CALENDAR)
                                    if (state.profile.contactsSearch) add(Manifest.permission.READ_CONTACTS)
                                    if (state.profile.weather) add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (state.profile.notificationBadges && Build.VERSION.SDK_INT >= 33) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                if (extras.isNotEmpty()) permission.launch(extras.toTypedArray())
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        DpadActionRow(
                            label = "Bitir ve ana ekrana geç",
                            emphasis = true,
                            focusRequester = if (state.profile.isChosen) startFocus else null,
                            onClick = viewModel::finishOnboarding,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditionPickCard(
    title: String,
    detail: String,
    selected: Boolean,
    suggested: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, label = "edition-focus")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(shape)
            .background(if (selected || focused) Color(0xCC101814) else Color(0x99101814))
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = when {
                    focused -> MaterialTheme.colorScheme.primary
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                },
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .padding(18.dp),
    ) {
        Text(title, color = Paper, style = MaterialTheme.typography.titleLarge)
        Text(detail, color = Paper.copy(alpha = 0.7f), fontSize = 14.sp)
        when {
            selected -> Text("Seçildi", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            suggested -> Text("Bu cihaz için önerilen", color = Paper.copy(alpha = 0.55f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun DpadActionRow(
    label: String,
    emphasis: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Text(
        text = label,
        color = Paper,
        fontSize = if (emphasis) 17.sp else 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(shape)
            .background(
                when {
                    focused && emphasis -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    emphasis -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                },
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
fun FolderOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    val folder = state.workspace.find { it.id == state.openFolderId && it.type == WorkspaceType.FOLDER }
    val members = folder?.members.orEmpty().mapNotNull { key ->
        state.apps.find { it.key == key } ?: state.vaultApps.find { it.key == key }
    }
    OverlayScrim(onClose = viewModel::closeOverlays) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                Column {
                    var title by remember(folder?.id, folder?.title) {
                        mutableStateOf(folder?.title?.ifBlank { "Klasör" } ?: "Klasör")
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { value ->
                            title = value
                            folder?.let { viewModel.renameFolder(it.id, value) }
                        },
                        textStyle = TextStyle(color = Paper, fontSize = 22.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("İsim yazın. Üyeyi çıkarmak için Çıkar’a basın. Rengi seçin veya önerilen uygulamayı ekleyin.", color = Paper.copy(alpha = 0.65f), fontSize = 12.sp)
                    Row(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                        listOf(0, 0xFF1B3A2F.toInt(), 0xFF3A2A1B.toInt(), 0xFF2A1B3A.toInt(), 0xFF1B2A3A.toInt()).forEach { color ->
                            val selected = (folder?.color ?: 0) == color
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (color == 0) Color(0xFF22332C) else Color(color))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .then(
                                        if (folder != null) {
                                            Modifier.dpadClickable { viewModel.setFolderColor(folder.id, color) }
                                        } else Modifier
                                    ),
                            )
                        }
                    }
                    com.makay.launcher.ui.components.DpadAction("Kapat", onClick = viewModel::closeOverlays)
                    folder?.let {
                        com.makay.launcher.ui.components.DpadAction("Klasörü sil") { viewModel.removeWorkspaceItem(it.id) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val suggested = state.apps
                .filter { app -> folder?.members?.contains(app.key) != true }
                .sortedByDescending { it.usageCount }
                .take(8)
            if (folder != null && suggested.isNotEmpty()) {
                Text("Önerilen", color = Paper.copy(alpha = 0.75f), fontSize = 13.sp)
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    suggested.take(4).forEach { app ->
                        com.makay.launcher.ui.components.DpadChip(app.label.take(12), false) {
                            viewModel.addFolderMember(folder.id, app.key)
                        }
                    }
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(4)) {
                items(members, key = { it.key }) { app ->
                    Column {
                        HomeApp(app, state, viewModel)
                        folder?.let { current ->
                            com.makay.launcher.ui.components.DpadAction("Çıkar") {
                                viewModel.removeFolderMember(current.id, app.key)
                            }
                        }
                    }
                }
            }
        }
    }
}
