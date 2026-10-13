package com.makay.launcher.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.domain.AppCategory
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.Overlay
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper

@Composable
fun TvHome(state: HomeUiState, viewModel: HomeViewModel) {
    val favorites = state.favorites.ifEmpty { state.apps.sortedByDescending { it.usageCount }.take(12) }
    val frequent = state.apps.sortedByDescending { it.usageCount }.take(14)
    val media = state.apps.filter { it.category == AppCategory.Media || it.category == AppCategory.Games }
    var clock by remember {
        mutableStateOf(java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(java.util.Date()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(java.util.Date())
            delay(15_000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                event.key == Key.ChannelUp || event.key == Key.ChannelDown
            },
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(clock, color = Paper, fontSize = 56.sp, style = MaterialTheme.typography.displaySmall)
            Text(
                if (state.isDefaultLauncher) "Kumanda ile gezinin · OK ile açın · Menü ile düzenleyin"
                else "Henüz varsayılan değil · Ayarlar’dan Ana ekran uygulamasını Makay yapın",
                color = Paper.copy(alpha = 0.7f),
            )
            if (state.glance.weather.isNotBlank() || state.glance.headline.isNotBlank()) {
                Text(
                    listOf(state.glance.weather, state.glance.headline).filter { it.isNotBlank() }.joinToString(" · "),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item {
            TvActionRow(state, viewModel)
        }
        item {
            TvRow(title = "Harfle ara") {
                ('A'..'Z').forEach { letter ->
                    TvFocusCard(letter.toString(), onClick = {
                        viewModel.open(Overlay.Search)
                        viewModel.onSearch(letter.toString())
                    })
                }
            }
        }
        val workApps = state.apps.filter { it.work }
        if (workApps.isNotEmpty()) {
            item {
                TvRow(title = "İş profili") {
                    workApps.forEach { app -> TvAppCard(app, state, viewModel) }
                }
            }
        }
        val privateApps = state.apps.filter { it.privateSpace }
        if (privateApps.isNotEmpty()) {
            item {
                TvRow(title = "Özel alan") {
                    privateApps.forEach { app -> TvAppCard(app, state, viewModel) }
                }
            }
        }
        if (state.inputSources.isNotEmpty()) {
            item {
                TvRow(title = "Giriş kaynakları") {
                    state.inputSources.forEach { source ->
                        TvFocusCard(label = source.label, onClick = { viewModel.launchInput(source) })
                    }
                }
            }
        }
        if (!state.settings.digitalDetox) {
            item {
                TvRow(title = "Sık kullanılanlar") {
                    frequent.forEach { app -> TvAppCard(app, state, viewModel) }
                }
            }
        }
        item {
            TvRow(title = "Favoriler") {
                favorites.forEach { app -> TvAppCard(app, state, viewModel) }
            }
        }
        if (media.isNotEmpty()) {
            item {
                TvRow(title = "Medya ve oyun") {
                    media.forEach { app -> TvAppCard(app, state, viewModel) }
                }
            }
        }
        AppCategory.entries
            .filter { it != AppCategory.Frequent }
            .forEach { category ->
                val apps = state.apps.filter { it.category == category }
                if (apps.isNotEmpty()) {
                    item {
                        TvRow(title = category.title) {
                            apps.forEach { app -> TvAppCard(app, state, viewModel) }
                        }
                    }
                }
            }
    }
}

@Composable
private fun TvActionRow(state: HomeUiState, viewModel: HomeViewModel) {
    TvRow(title = "Hızlı erişim") {
        TvFocusCard("Tüm uygulamalar", onClick = { viewModel.open(Overlay.Drawer) })
        TvFocusCard("Ara", onClick = { viewModel.open(Overlay.Search) })
        TvFocusCard("Ayarlar", onClick = { viewModel.open(Overlay.Settings) })
        TvFocusCard("Ekran koruyucu", onClick = { viewModel.openDreamSettings() })
        TvFocusCard("Duvar kağıdı", onClick = { viewModel.openWallpaperPicker() })
        if (!state.isDefaultLauncher) {
            TvFocusCard("Varsayılan yap", onClick = { viewModel.openDefaultHomePicker() })
        }
    }
}

@Composable
private fun TvRow(title: String, content: @Composable RowScope.() -> Unit) {
    Column {
        Text(title, color = Paper, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun TvAppCard(app: AppInfo, state: HomeUiState, viewModel: HomeViewModel) {
    TvFocusCard(label = app.label, onClick = { viewModel.launch(app) }, onLongClick = { viewModel.openShutter(app) }) {
        HomeApp(app, state, viewModel, showLabel = false, iconSize = 72.dp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TvFocusCard(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    leading: @Composable () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "tv-focus")
    Column(
        modifier = Modifier
            .width(if (label.length <= 2) 76.dp else 168.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else GlassStrong)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.DirectionCenter)) {
                    onClick()
                    true
                } else if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) {
                    onLongClick()
                    true
                } else {
                    false
                }
            }
            .padding(14.dp),
    ) {
        leading()
        Text(label, color = Paper, maxLines = 2, fontSize = 16.sp)
    }
}
