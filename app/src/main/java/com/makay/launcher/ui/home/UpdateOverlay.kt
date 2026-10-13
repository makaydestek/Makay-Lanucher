package com.makay.launcher.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.BuildConfig
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.DpadAction
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.theme.Paper

@Composable
fun UpdateOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    val offer = state.pendingUpdate ?: return
    OverlayScrim(onClose = viewModel::postponeUpdate) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                Column {
                    Text("Güncelleme var", color = Paper, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Yüklü: ${BuildConfig.VERSION_NAME}  →  Yeni: ${offer.versionName}",
                        color = Paper.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                    )
                    if (offer.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(offer.notes, color = Paper.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                    if (state.updateStatus.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.updateStatus, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    val startFocus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { startFocus.requestFocus() } }
                    DpadAction("Güncelle", emphasis = true, focusRequester = startFocus, onClick = viewModel::applyPendingUpdate)
                    DpadAction("Ertele (24 saat)", onClick = viewModel::postponeUpdate)
                }
            }
        }
    }
}
