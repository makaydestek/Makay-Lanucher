@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.makay.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.domain.AppCategory
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.AppIconButton
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.components.tileColor
import com.makay.launcher.ui.theme.Paper
import kotlinx.coroutines.launch

@Composable
fun GridHome(state: HomeUiState, viewModel: HomeViewModel) {
    WorkspaceGrid(state, viewModel)
}

@Composable
fun NiagaraHome(state: HomeUiState, viewModel: HomeViewModel) {
    val frequent = state.apps.sortedByDescending { it.usageCount }.take(6)
    val grouped = remember(state.apps) {
        state.apps.groupBy { it.label.firstOrNull()?.uppercaseChar() ?: '#' }
            .toSortedMap()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val letters = grouped.keys.toList()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!state.settings.digitalDetox) {
                frequent.forEach { app ->
                    HomeApp(app, state, viewModel, showLabel = false, iconSize = 48.dp)
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            grouped.forEach { (letter, apps) ->
                item(key = "h$letter") {
                    Text(
                        letter.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(apps, key = { it.key }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.launch(app) },
                                onLongClick = { viewModel.openShutter(app) },
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!state.settings.textOnly) {
                            HomeApp(app, state, viewModel, showLabel = false, iconSize = 36.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(app.label, color = Paper, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        val badge = state.badges[app.packageName] ?: 0
                        if (badge > 0) {
                            Text(
                                badge.toString(),
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE24B4A))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        if (state.settings.showAlphabetRail) {
            Column(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                letters.forEachIndexed { index, letter ->
                    Text(
                        letter.toString(),
                        color = Paper.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                val offset = grouped.entries.take(index).sumOf { 1 + it.value.size }
                                scope.launch { listState.scrollToItem(offset) }
                            },
                            onLongClick = {},
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TileHome(state: HomeUiState, viewModel: HomeViewModel) {
    val apps = state.favorites.ifEmpty { state.apps.take(10) }
    if (apps.isEmpty()) {
        EmptyHome()
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        apps.forEachIndexed { index, app ->
            val large = index < 2 || app.usageCount > 3
            Box(
                modifier = Modifier
                    .size(if (large) 156.dp else 74.dp, if (large) 74.dp else 74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileColor(app.packageName))
                    .combinedClickable(
                        onClick = { viewModel.launch(app) },
                        onLongClick = { viewModel.openShutter(app) },
                    )
                    .padding(12.dp),
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(app.label, color = Color.White, maxLines = 2, fontSize = 13.sp)
                    if (large && app.packageName.contains("clock", true)) {
                        Text(
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHome(state: HomeUiState, viewModel: HomeViewModel) {
    val stats = state.deviceStats
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            StatCard("Pil", "%${stats.batteryPercent}", stats.batteryPercent / 100f)
        }
        item {
            val ratio = if (stats.totalStorageGb == 0f) 0f else stats.usedStorageGb / stats.totalStorageGb
            StatCard("Depolama", "${"%.1f".format(stats.usedStorageGb)} / ${"%.0f".format(stats.totalStorageGb)} GB", ratio)
        }
        item {
            val used = (stats.totalMemGb - stats.availMemGb).coerceAtLeast(0f)
            val ratio = if (stats.totalMemGb == 0f) 0f else used / stats.totalMemGb
            StatCard("Bellek", "${"%.1f".format(used)} / ${"%.1f".format(stats.totalMemGb)} GB", ratio)
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Sık kullanılanlar", color = Paper, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        state.apps.sortedByDescending { it.usageCount }.take(5).forEach { app ->
                            HomeApp(app, state, viewModel, showLabel = true, iconSize = 44.dp)
                        }
                    }
                }
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Kategoriler", color = Paper, fontWeight = FontWeight.SemiBold)
                    AppCategory.entries.filter { it != AppCategory.Frequent }.forEach { category ->
                        val count = state.apps.count { it.category == category }
                        if (count > 0) {
                            Text(
                                "${category.title}: $count uygulama",
                                color = Paper.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextHome(state: HomeUiState, viewModel: HomeViewModel) {
    val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(java.util.Date())
    val items = state.favorites.ifEmpty { state.apps.sortedByDescending { it.usageCount }.take(6) }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(now, color = Paper, fontSize = 64.sp)
        Spacer(Modifier.height(28.dp))
        items.forEach { app ->
            Text(
                app.label,
                color = Paper,
                fontSize = 22.sp,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .combinedClickable(
                        onClick = { viewModel.launch(app) },
                        onLongClick = { viewModel.openShutter(app) },
                    ),
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, progress: Float) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, color = Paper.copy(alpha = 0.7f))
            Text(value, color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyHome() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Çekmeceden uygulamayı uzun basıp ana ekrana ekleyin.", color = Paper.copy(alpha = 0.75f))
    }
}

@Composable
fun HomeApp(
    app: AppInfo,
    state: HomeUiState,
    viewModel: HomeViewModel,
    showLabel: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 52.dp,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    AppIconButton(
        app = if (label != null) app.copy(label = label) else app,
        textOnly = state.settings.textOnly,
        badge = state.badges[app.packageName] ?: 0,
        iconSize = iconSize,
        showLabel = showLabel,
        iconShape = state.settings.iconShape,
        iconPack = state.settings.iconPackPackage,
        onClick = onClick ?: { viewModel.launch(app) },
        onLongClick = { viewModel.openShutter(app) },
    )
}
