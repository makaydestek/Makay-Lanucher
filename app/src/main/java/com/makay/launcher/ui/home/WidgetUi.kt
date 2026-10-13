@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.makay.launcher.ui.home

import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.makay.launcher.MakayApp
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.PinnedWidget
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.DpadAction
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.components.dpadClickable
import com.makay.launcher.ui.theme.Paper

@Composable
fun WidgetStrip(state: HomeUiState, viewModel: HomeViewModel) {
    if (state.widgets.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.widgets.forEach { widget ->
            WidgetFrame(
                widget = widget,
                onRemove = { viewModel.removeWidget(widget) },
                onRebind = { viewModel.rebindWidget(widget.provider) },
            )
        }
    }
}

@Composable
fun WidgetFrame(
    widget: PinnedWidget,
    onRemove: () -> Unit,
    onRebind: () -> Unit = {},
    widthDp: Int = 0,
    heightDp: Int = 0,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MakayApp
    val info = remember(widget.appWidgetId) {
        app.widgetController.infoOrNull(widget.appWidgetId)
    }
    if (info == null) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Widget bağlantısı koptu. Kimlik ${widget.appWidgetId}", color = Paper)
            Text("Yedekten sonra izin vermeniz gerekebilir.", color = Paper.copy(alpha = 0.65f), fontSize = 12.sp)
            DpadAction("Yeniden bağla", onClick = onRebind)
            DpadAction("Kaldır", onClick = onRemove)
        }
        return
    }
    val minHeight = info.minHeight.coerceIn(110, 280)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                app.widgetController.host.createView(ctx, widget.appWidgetId, info).apply {
                    setAppWidget(widget.appWidgetId, info)
                    setPadding(0, 0, 0, 0)
                }
            },
            update = { view: AppWidgetHostView ->
                view.setAppWidget(widget.appWidgetId, info)
                if (widthDp > 0 && heightDp > 0) {
                    app.widgetController.updateWidgetSize(
                        widget.appWidgetId,
                        widthDp,
                        heightDp,
                        widthDp,
                        heightDp,
                    )
                }
            },
        )
    }
}

@Composable
fun WidgetPicker(state: HomeUiState, viewModel: HomeViewModel) {
    OverlayScrim(onClose = viewModel::closeOverlays) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
                Column {
                    Text("Widget ekle", color = Paper, style = MaterialTheme.typography.titleLarge)
                    Text("Uzun basarak ana ekrandan kaldırabilirsiniz.", color = Paper.copy(alpha = 0.65f), fontSize = 12.sp)
                    DpadAction("Kapat", onClick = viewModel::closeOverlays)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (state.widgetOffers.isEmpty()) {
                Text("Yüklü widget bulunamadı.", color = Paper.copy(alpha = 0.65f), fontSize = 13.sp)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.widgetOffers, key = { it.providerFlattened }) { offer ->
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dpadClickable { viewModel.addWidget(offer) },
                    ) {
                        Column {
                            Text(offer.label, color = Paper)
                            Text(
                                "${offer.minWidth} × ${offer.minHeight}  ·  ${offer.providerFlattened.substringBefore("/")}",
                                color = Paper.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
