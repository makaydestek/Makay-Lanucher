@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.makay.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.domain.AppCategory
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.Overlay
import com.makay.launcher.domain.SearchResult
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.DpadAction
import com.makay.launcher.ui.components.DpadChip
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.components.dpadClickable
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper
import androidx.compose.ui.focus.onFocusChanged

@Composable
fun DockBar(state: HomeUiState, viewModel: HomeViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(GlassStrong)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.dock.forEach { app ->
            Box(
                modifier = Modifier.pointerInput(app.key) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -24) viewModel.openShutter(app)
                    }
                },
            ) {
                HomeApp(app, state, viewModel, showLabel = false, iconSize = 48.dp)
            }
        }
    }
}

@Composable
fun AppDrawer(state: HomeUiState, viewModel: HomeViewModel, embedded: Boolean = false) {
    var category by remember { mutableStateOf<AppCategory?>(null) }
    var workOnly by remember { mutableStateOf(false) }
    val filtered = remember(state.apps, category, workOnly) {
        when {
            workOnly -> state.apps.filter { it.work }
            category == null -> state.apps
            category == AppCategory.Frequent -> state.apps.sortedByDescending { it.usageCount }.take(24)
            else -> state.apps.filter { it.category == category }
        }
    }
    val body = @Composable {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (embedded) Modifier else Modifier.statusBarsPadding().navigationBarsPadding())
                .padding(if (embedded) 8.dp else 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Uygulamalar", color = Paper, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                DpadAction("Ara") { viewModel.open(Overlay.Search) }
                if (!embedded) {
                    DpadAction("Kapat", onClick = viewModel::closeOverlays)
                }
            }
            val workApps = state.apps.filter { it.work }
            if (state.settings.autoCategories || workApps.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        DpadChip("Tümü", category == null && !workOnly) {
                            category = null
                            workOnly = false
                        }
                    }
                    if (state.settings.autoCategories) {
                        items(AppCategory.entries) { item ->
                            DpadChip(item.title, !workOnly && category == item) {
                                workOnly = false
                                category = item
                            }
                        }
                    }
                    if (workApps.isNotEmpty()) {
                        item {
                            DpadChip("İş", workOnly) {
                                workOnly = true
                                category = null
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (state.settings.textOnly) {
                LazyColumn {
                    items(filtered, key = { it.key }) { app ->
                        Text(
                            app.label,
                            color = Paper,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .dpadClickable(
                                    onClick = { viewModel.launch(app) },
                                    onLongClick = { viewModel.openShutter(app) },
                                )
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            } else {
                val columns = if (state.isWideScreen) 6 else 4
                LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
                    items(filtered, key = { it.key }) { app ->
                        HomeApp(app, state, viewModel)
                    }
                }
            }
        }
    }
    if (embedded) {
        GlassPanel(modifier = Modifier.fillMaxSize(), strong = true) { body() }
    } else {
        OverlayScrim(onClose = viewModel::closeOverlays) { body() }
    }
}

@Composable
fun VaultOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    OverlayScrim(onClose = viewModel::closeOverlays) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Text("Gizli kasa", color = Paper, style = MaterialTheme.typography.headlineSmall)
            Text("Biyometrik doğrulama sonrası görünür.", color = Paper.copy(alpha = 0.65f), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            if (state.vaultApps.isEmpty()) {
                Text("Kasa boş. Shutter menüsünden “Kasaya gizle” deyin.", color = Paper)
            } else {
                val columns = if (state.isWideScreen) 6 else 4
                LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
                    items(state.vaultApps, key = { it.key }) { app ->
                        HomeApp(app, state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { searchFocus.requestFocus() } }
    OverlayScrim(onClose = viewModel::closeOverlays) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearch,
                    textStyle = TextStyle(color = Paper, fontSize = 18.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocus),
                    decorationBox = { inner ->
                        if (state.searchQuery.isEmpty()) {
                            Text(
                                if (state.profile.contactsSearch) {
                                    "Uygulama, kısayol, kişi veya eylem ara"
                                } else {
                                    "Uygulama, kısayol veya eylem ara"
                                },
                                color = Paper.copy(alpha = 0.5f),
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            if (state.profile.isTv && state.searchQuery.isEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(('A'..'Z').toList()) { letter ->
                        DpadChip(letter.toString(), false) { viewModel.onSearch(letter.toString()) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.searchResults) { result ->
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadClickable {
                                when (result.kind) {
                                    SearchResult.Kind.App -> result.app?.let(viewModel::launch)
                                    else -> result.action?.invoke()
                                }
                            },
                    ) {
                        Column {
                            Text(result.title, color = Paper)
                            Text(result.subtitle, color = Paper.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlCenter(state: HomeUiState, viewModel: HomeViewModel) {
    OverlayScrim(onClose = viewModel::closeOverlays) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                Column {
                    Text("Kontrol merkezi", color = Paper, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DpadAction("Wi-Fi") { viewModel.openWifiSettings() }
                        DpadAction("Bluetooth") { viewModel.openBluetoothSettings() }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DpadAction(if (state.flashlightOn) "Fener açık" else "Fener") { viewModel.toggleFlashlight() }
                        DpadAction("Bildirimler") { viewModel.expandNotifications() }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DpadAction("Sistem") { viewModel.openSystemSettings() }
                        DpadAction("Ayarlar") { viewModel.open(Overlay.Settings) }
                    }
                }
            }
        }
    }
}

@Composable
fun ShutterSheet(app: AppInfo, state: HomeUiState, viewModel: HomeViewModel) {
    OverlayScrim(onClose = viewModel::closeOverlays) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                Column {
                    Text(app.label, color = Paper, style = MaterialTheme.typography.titleLarge)
                    Text("Action tarzı shutter / kısayollar", color = Paper.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    SheetAction("Aç") { viewModel.launch(app) }
                    SheetAction(if (state.favorites.any { it.key == app.key }) "Ana ekrandan kaldır" else "Ana ekrana ekle") {
                        viewModel.toggleFavorite(app)
                    }
                    if (state.profile.isTv && state.favorites.any { it.key == app.key }) {
                        SheetAction("Sola taşı") { viewModel.moveFavorite(app, -1) }
                        SheetAction("Sağa taşı") { viewModel.moveFavorite(app, 1) }
                    }
                    if (state.profile.isTv) {
                        SheetAction("Kartı özelleştir") { viewModel.openTvAppEdit(app) }
                        SheetAction("Program detayı") { viewModel.openProgramDetail(app) }
                        SheetAction(
                            if (state.settings.tvBlockedPackages.split(',').map { it.trim() }.contains(app.packageName)) {
                                "Ebeveyn engelini kaldır"
                            } else {
                                "Ebeveyn ile engelle"
                            },
                        ) { viewModel.toggleBlockedPackage(app.packageName) }
                    }
                    if (state.profile.dock) {
                        SheetAction(if (state.dock.any { it.key == app.key }) "Dock’tan kaldır" else "Dock’a ekle") {
                            viewModel.toggleDock(app)
                        }
                    }
                    SheetAction("Uygulama bilgisi") { viewModel.openAppInfo(app) }
                    if (!app.isSystem) {
                        SheetAction("Kaldır") { viewModel.uninstall(app) }
                    }
                    if (state.profile.vault) {
                        SheetAction(if (state.hidden.contains(app.packageName)) "Kasadan çıkar" else "Kasaya gizle") {
                            viewModel.toggleHidden(app)
                        }
                    }
                    if (state.profile.biometric) {
                        SheetAction(if (state.locked.contains(app.packageName)) "Kilidi kaldır" else "Biyometrik kilit") {
                            viewModel.toggleLocked(app)
                        }
                    }
                    state.shortcuts.forEach { shortcut ->
                        SheetAction(shortcut.label) { viewModel.launchShortcut(app, shortcut) }
                        if (state.profile.workspaceGrid) {
                            SheetAction("${shortcut.label} · ana ekrana sabitle") {
                                viewModel.pinShortcut(app, shortcut)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OverlayScrim(
    onClose: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .focusProperties { canFocus = false }
                .pointerInput(onClose) {
                    detectTapGestures { onClose() }
                },
        )
        content()
    }
}

@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, color = Paper)
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Paper,
        modifier = Modifier
            .fillMaxWidth()
            .dpadClickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}
