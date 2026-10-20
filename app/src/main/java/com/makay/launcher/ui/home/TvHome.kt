package com.makay.launcher.ui.home

import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.Overlay
import com.makay.launcher.domain.TvBuiltinRow
import com.makay.launcher.domain.TvFocusEffect
import com.makay.launcher.domain.TvResolvedRow
import com.makay.launcher.domain.TvWallpaperKind
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun TvHome(state: HomeUiState, viewModel: HomeViewModel) {
    var clock by remember {
        mutableStateOf(java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(java.util.Date()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(java.util.Date())
            delay(15_000)
        }
    }
    val listState = rememberLazyListState()
    val defaultRowId = remember(state.tvRows) {
        state.tvRows.firstOrNull { it.isDefault }?.id
    }
    LaunchedEffect(defaultRowId, state.tvResolvedRows) {
        if (defaultRowId == null) return@LaunchedEffect
        val index = state.tvResolvedRows.indexOfFirst { it.def.id == defaultRowId }
        if (index >= 0) {
            // +1 for clock header
            listState.animateScrollToItem(index + 1)
        }
    }
    LaunchedEffect(listState, state.settings.tvHideRowsAboveFocus) {
        if (!state.settings.tvHideRowsAboveFocus) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { visible ->
                val rowIndex = (visible - 1).coerceAtLeast(0)
                val rowId = state.tvResolvedRows.getOrNull(rowIndex)?.def?.id
                if (rowId != null) viewModel.setTvFocusedRow(rowId)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TvWallpaperLayer(state)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    viewModel.bumpIdle()
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
            items(state.tvResolvedRows, key = { it.def.id }) { row ->
                TvResolvedRowUi(row, state, viewModel)
            }
        }
    }
}

@Composable
private fun TvWallpaperLayer(state: HomeUiState) {
    val settings = state.settings
    if (settings.tvWallpaperKind == TvWallpaperKind.NONE || settings.tvWallpaperUri.isBlank()) return
    val uri = Uri.parse(settings.tvWallpaperUri)
    val blurMod = if (settings.tvWallpaperBlur > 0 && Build.VERSION.SDK_INT >= 31) {
        Modifier.blur(settings.tvWallpaperBlur.dp)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(blurMod)
            .graphicsLayer { alpha = settings.tvWallpaperBrightness.coerceIn(0.2f, 1f) },
    ) {
        when (settings.tvWallpaperKind) {
            TvWallpaperKind.VIDEO -> TvVideoWallpaper(uri)
            TvWallpaperKind.IMAGE, TvWallpaperKind.GIF -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            TvWallpaperKind.NONE -> Unit
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = settings.tvWallpaperDim.coerceIn(0f, 0.85f))),
        )
    }
}

@Composable
private fun TvVideoWallpaper(uri: Uri) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setOnPreparedListener { player ->
                    player.isLooping = true
                    player.setVolume(0f, 0f)
                    start()
                }
                setVideoURI(uri)
            }
        },
        update = { view ->
            if (view.tag != uri.toString()) {
                view.tag = uri.toString()
                view.setVideoURI(uri)
                view.start()
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
    DisposableEffect(uri) {
        onDispose { }
    }
}

@Composable
private fun TvResolvedRowUi(row: TvResolvedRow, state: HomeUiState, viewModel: HomeViewModel) {
    when {
        row.isActions -> TvActionRow(state, viewModel)
        row.isLetters -> TvRow(title = row.def.displayTitle) {
            ('A'..'Z').forEach { letter ->
                TvFocusCard(letter.toString(), state, viewModel, onClick = {
                    viewModel.open(Overlay.Search)
                    viewModel.onSearch(letter.toString())
                })
            }
        }
        row.inputs.isNotEmpty() -> TvRow(title = row.def.displayTitle) {
            row.inputs.forEach { source ->
                TvFocusCard(label = source.label, state = state, viewModel = viewModel, onClick = {
                    viewModel.launchInput(source)
                })
            }
        }
        else -> TvRow(title = row.def.displayTitle) {
            row.apps.forEach { app ->
                val watch = row.def.builtin == TvBuiltinRow.WATCH_NEXT
                TvAppCard(app, state, viewModel, showProgress = watch)
            }
        }
    }
}

@Composable
private fun TvActionRow(state: HomeUiState, viewModel: HomeViewModel) {
    TvRow(title = "Hızlı erişim") {
        TvFocusCard("Tüm uygulamalar", state, viewModel, onClick = { viewModel.open(Overlay.Drawer) })
        TvFocusCard("Ara", state, viewModel, onClick = { viewModel.open(Overlay.Search) })
        TvFocusCard("Ayarlar", state, viewModel, onClick = { viewModel.open(Overlay.Settings) })
        TvFocusCard("Satırlar", state, viewModel, onClick = { viewModel.open(Overlay.TvRows) })
        TvFocusCard("Ekran koruyucu", state, viewModel, onClick = { viewModel.openDreamSettings() })
        TvFocusCard("Duvar kağıdı", state, viewModel, onClick = { viewModel.openWallpaperPicker() })
        if (!state.isDefaultLauncher) {
            TvFocusCard("Varsayılan yap", state, viewModel, onClick = { viewModel.openDefaultHomePicker() })
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
private fun TvAppCard(
    app: AppInfo,
    state: HomeUiState,
    viewModel: HomeViewModel,
    showProgress: Boolean = false,
) {
    val progress = if (showProgress) {
        ((app.usageCount % 10) / 10f).coerceIn(0.15f, 0.95f)
    } else {
        0f
    }
    TvFocusCard(
        label = app.label,
        state = state,
        viewModel = viewModel,
        onClick = { viewModel.launch(app) },
        onLongClick = { viewModel.openShutter(app) },
        subtitle = if (showProgress) "İzlemeye devam" else null,
        progress = progress,
    ) {
        HomeApp(app, state, viewModel, showLabel = false, iconSize = state.settings.tvCardSize.iconDp.dp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun TvFocusCard(
    label: String,
    state: HomeUiState,
    viewModel: HomeViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    subtitle: String? = null,
    progress: Float = 0f,
    leading: @Composable () -> Unit = {},
) {
    val settings = state.settings
    var focused by remember { mutableStateOf(false) }
    val scaleTarget = when (settings.tvFocusEffect) {
        TvFocusEffect.SCALE, TvFocusEffect.GLOW, TvFocusEffect.SHADOW -> if (focused) 1.08f else 1f
        TvFocusEffect.NONE -> 1f
    }
    val scale by animateFloatAsState(scaleTarget, label = "tv-focus")
    val corner = RoundedCornerShape(settings.tvCardCorner.coerceIn(4, 32).dp)
    val width = if (label.length <= 2) 76.dp else settings.tvCardSize.widthDp.dp
    val bgAlpha = settings.tvCardOpacity.coerceIn(0.25f, 1f)
    val glow = focused && settings.tvFocusEffect == TvFocusEffect.GLOW
    val shadow = focused && settings.tvFocusEffect == TvFocusEffect.SHADOW
    Column(
        modifier = Modifier
            .width(width)
            .scale(scale)
            .then(if (shadow) Modifier.shadow(16.dp, corner) else Modifier)
            .clip(corner)
            .background(
                if (focused) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f.coerceAtMost(bgAlpha))
                } else {
                    GlassStrong.copy(alpha = bgAlpha)
                },
            )
            .then(
                if (glow) {
                    Modifier.border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.primary,
                            ),
                        ),
                        shape = corner,
                    )
                } else {
                    Modifier.border(
                        width = if (focused) 3.dp else 1.dp,
                        color = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                        shape = corner,
                    )
                },
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) viewModel.bumpIdle()
            }
            .focusable()
            .onPreviewKeyEvent { event ->
                viewModel.bumpIdle()
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
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = Paper.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1)
        }
        if (progress > 0f) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
        }
    }
}
