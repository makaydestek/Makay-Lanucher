package com.makay.launcher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.makay.launcher.domain.HomeStyle
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.ui.HomeViewModel

@Composable
fun TabletHome(
    state: HomeUiState,
    viewModel: HomeViewModel,
    glance: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight()
                .padding(end = 12.dp),
        ) {
            actions()
            glance()
            Spacer(Modifier.height(8.dp))
            if (state.profile.widgets &&
                state.workspace.none { it.type == com.makay.launcher.domain.WorkspaceType.WIDGET }
            ) {
                WidgetStrip(state, viewModel)
                if (state.widgets.isNotEmpty()) Spacer(Modifier.height(8.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                when (state.settings.homeStyle) {
                    HomeStyle.GRID -> GridHome(state, viewModel)
                    HomeStyle.NIAGARA -> NiagaraHome(state, viewModel)
                    HomeStyle.TILES -> TileHome(state, viewModel)
                    HomeStyle.DASHBOARD -> DashboardHome(state, viewModel)
                    HomeStyle.TEXT -> TextHome(state, viewModel)
                }
            }
            if (state.settings.showDock && state.settings.homeStyle != HomeStyle.NIAGARA) {
                DockBar(state, viewModel)
            }
        }
        Box(
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight(),
        ) {
            AppDrawer(state, viewModel, embedded = true)
        }
    }
}
