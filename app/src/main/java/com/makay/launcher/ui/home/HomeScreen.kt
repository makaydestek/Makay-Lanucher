package com.makay.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.domain.HomeStyle
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.Overlay
import com.makay.launcher.domain.WorkspaceType
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.DpadAction
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.settings.SettingsScreen
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper

@Composable
fun HomeScreen(
    state: HomeUiState,
    viewModel: HomeViewModel,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    LaunchedEffect(widthDp) {
        viewModel.setWideScreen(widthDp >= 600)
        viewModel.refreshFormFactor()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.profile.gestures) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.TopCenter)
                    .pointerInput(state.settings.swipeDown) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 28) viewModel.perform(state.settings.swipeDown)
                        }
                    },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (state.profile.isTv) 32.dp else 16.dp),
        ) {
            if (state.profile.isChosen && !state.isDefaultLauncher) {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Makay Launcher henüz varsayılan değil", color = Paper)
                        if (state.profile.isTv) {
                            Text(
                                "TV: Ayarlar → Cihaz tercihleri → Ana ekran / Başlatıcı → Makay Launcher.",
                                color = Paper.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                        }
                        DpadAction("Varsayılan ana ekran yap", emphasis = true, onClick = viewModel::openDefaultHomePicker)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            when {
                !state.profile.isChosen -> Spacer(Modifier.weight(1f))
                state.profile.isTv -> {
                    Box(modifier = Modifier.weight(1f)) {
                        TvHome(state, viewModel)
                    }
                }
                state.isWideScreen -> {
                    Box(modifier = Modifier.weight(1f)) {
                        TabletHome(
                            state = state,
                            viewModel = viewModel,
                            actions = { TopActions(state, viewModel) },
                            glance = {
                                if (state.profile.glanceSmart &&
                                    state.settings.showAtAGlance &&
                                    state.settings.homeStyle != HomeStyle.TEXT
                                ) {
                                    AtAGlance(state, viewModel, onDoubleTap = { viewModel.perform(state.settings.doubleTap) })
                                    Spacer(Modifier.height(12.dp))
                                }
                            },
                        )
                    }
                }
                else -> {
                    TopActions(state, viewModel)
                    if (state.profile.glanceSmart &&
                        state.settings.showAtAGlance &&
                        state.settings.homeStyle != HomeStyle.TEXT
                    ) {
                        AtAGlance(state, viewModel, onDoubleTap = { viewModel.perform(state.settings.doubleTap) })
                        Spacer(Modifier.height(12.dp))
                    }
                    if (state.profile.widgets &&
                        state.workspace.none { it.type == WorkspaceType.WIDGET } &&
                        state.widgets.isNotEmpty()
                    ) {
                        WidgetStrip(state, viewModel)
                        Spacer(Modifier.height(10.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.profile.mobileHomeStyles) {
                            when (state.settings.homeStyle) {
                                HomeStyle.GRID -> GridHome(state, viewModel)
                                HomeStyle.NIAGARA -> NiagaraHome(state, viewModel)
                                HomeStyle.TILES -> TileHome(state, viewModel)
                                HomeStyle.DASHBOARD -> DashboardHome(state, viewModel)
                                HomeStyle.TEXT -> TextHome(state, viewModel)
                            }
                        } else {
                            GridHome(state, viewModel)
                        }
                    }
                    if (state.profile.dock &&
                        state.settings.showDock &&
                        state.settings.homeStyle != HomeStyle.NIAGARA
                    ) {
                        DockBar(state, viewModel)
                    }
                    if (state.profile.gestures && !state.gestureNavigation) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .pointerInput(state.settings.swipeUp) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount < -20) viewModel.perform(state.settings.swipeUp)
                                    }
                                },
                        )
                    }
                }
            }
        }

        when (state.overlay) {
            Overlay.None -> Unit
            Overlay.Drawer -> if (!state.isWideScreen || state.profile.isTv) AppDrawer(state, viewModel)
            Overlay.Search -> if (state.profile.search) SearchOverlay(state, viewModel)
            Overlay.ControlCenter -> if (state.profile.controlCenter) ControlCenter(state, viewModel)
            Overlay.Settings -> SettingsScreen(state, viewModel)
            Overlay.WidgetPicker -> if (state.profile.widgets) WidgetPicker(state, viewModel)
            Overlay.Vault -> if (state.profile.vault) VaultOverlay(state, viewModel)
            Overlay.Folder -> if (state.profile.folders) FolderOverlay(state, viewModel)
            Overlay.Onboarding -> OnboardingOverlay(state, viewModel)
            Overlay.Update -> UpdateOverlay(state, viewModel)
            Overlay.Idle -> if (state.profile.isTv) TvIdleOverlay(viewModel)
            Overlay.Calibration -> if (state.profile.isTv) CalibrationOverlay(state, viewModel)
            Overlay.ParentalPin -> ParentalPinOverlay(state, viewModel)
            Overlay.TvRows -> if (state.profile.isTv) TvRowsEditor(state, viewModel)
            Overlay.AllApps -> if (state.profile.isTv) AllAppsPanel(state, viewModel)
            Overlay.ProgramDetail -> if (state.profile.isTv) ProgramDetailPanel(state, viewModel)
            Overlay.Sideload -> if (state.profile.isTv) SideloadOverlay(state, viewModel)
            Overlay.TvAppEdit -> if (state.profile.isTv) TvAppEditOverlay(state, viewModel)
            Overlay.AutoBackups -> if (state.profile.isTv) AutoBackupsOverlay(state, viewModel)
        }

        state.shutterApp?.let { ShutterSheet(it, state, viewModel) }
    }
}

@Composable
private fun TopActions(state: HomeUiState, viewModel: HomeViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionChip(androidx.compose.ui.res.stringResource(com.makay.launcher.R.string.action_search)) { viewModel.open(Overlay.Search) }
        if (!state.isWideScreen) {
            ActionChip(androidx.compose.ui.res.stringResource(com.makay.launcher.R.string.action_drawer)) { viewModel.open(Overlay.Drawer) }
        }
        if (state.profile.widgets) {
            ActionChip(androidx.compose.ui.res.stringResource(com.makay.launcher.R.string.action_widgets)) { viewModel.openWidgetPicker() }
        }
        if (state.profile.vault && state.hidden.isNotEmpty()) {
            ActionChip(androidx.compose.ui.res.stringResource(com.makay.launcher.R.string.action_vault)) { viewModel.openVault() }
        }
        if (state.profile.controlCenter) {
            ActionChip(androidx.compose.ui.res.stringResource(com.makay.launcher.R.string.action_control_center)) { viewModel.open(Overlay.ControlCenter) }
        }
        ActionChip(androidx.compose.ui.res.stringResource(com.makay.launcher.R.string.action_settings)) { viewModel.open(Overlay.Settings) }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Paper,
        fontSize = 12.sp,
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(CircleShape)
            .background(GlassStrong)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun AtAGlance(state: HomeUiState, viewModel: HomeViewModel, onDoubleTap: () -> Unit) {
    val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    val date = java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(java.util.Date())
    val glance = state.glance
    val stats = state.deviceStats
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleTap() })
            },
    ) {
        Column {
            Text("${glance.greeting} · $now", color = Paper, fontSize = 36.sp, style = MaterialTheme.typography.displaySmall)
            Text(date.replaceFirstChar { it.uppercase() }, color = Paper.copy(alpha = 0.8f))
            Text(
                glance.headline.ifBlank { "Bir bakışta" },
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                glance.detail.ifBlank {
                    "Pil %${stats.batteryPercent}${if (stats.charging) " • şarj" else ""}"
                },
                color = Paper.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            if (glance.chips.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    glance.chips.forEach { chip ->
                        Text(
                            chip.label,
                            color = Paper,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                                .clickable { chip.app?.let(viewModel::launch) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
