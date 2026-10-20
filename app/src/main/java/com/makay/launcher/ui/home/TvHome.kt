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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import com.makay.launcher.domain.WatchNextItem
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

    val fadeAlpha = if (state.tvIdleFaded) 0.45f else 1f
    Box(modifier = Modifier.fillMaxSize()) {
        TvWallpaperLayer(state)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fadeAlpha }
                .onPreviewKeyEvent { event ->
                    viewModel.bumpIdle()
                    event.key == Key.ChannelUp || event.key == Key.ChannelDown
                },
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                if (state.settings.tvShowClock) {
                    Text(clock, color = Paper, fontSize = 56.sp, style = MaterialTheme.typography.displaySmall)
                }
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
    val focusedPreview = state.tvFocusedAppKey?.let { key ->
        state.tvOverrides[key]?.previewUri?.ifBlank { null }
            ?: state.tvOverrides[key]?.customIconUri?.ifBlank { null }
    }
    val rawUri = when (settings.tvWallpaperKind) {
        TvWallpaperKind.NONE -> ""
        TvWallpaperKind.FOCUSED -> focusedPreview.orEmpty().ifBlank { settings.tvWallpaperUri }
        else -> settings.tvWallpaperUri
    }
    if (settings.tvWallpaperKind == TvWallpaperKind.NONE || rawUri.isBlank()) return
    val uri = Uri.parse(rawUri)
    val blurMod = if (settings.tvWallpaperBlur > 0 && Build.VERSION.SDK_INT >= 31) {
        Modifier.blur(settings.tvWallpaperBlur.dp)
    } else {
        Modifier
    }
    val matrix = remember(settings.tvWallpaperContrast, settings.tvWallpaperSaturation, settings.tvWallpaperHue) {
        colorAdjustMatrix(settings.tvWallpaperContrast, settings.tvWallpaperSaturation, settings.tvWallpaperHue)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(blurMod)
            .graphicsLayer { alpha = settings.tvWallpaperBrightness.coerceIn(0.2f, 1f) },
    ) {
        when (settings.tvWallpaperKind) {
            TvWallpaperKind.VIDEO, TvWallpaperKind.RTSP -> TvVideoWallpaper(uri, settings.tvVideoZoom)
            TvWallpaperKind.URL -> {
                if (rawUri.lowercase().startsWith("rtsp") || rawUri.contains(".mp4")) {
                    TvVideoWallpaper(uri, settings.tvVideoZoom)
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(matrix),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            TvWallpaperKind.IMAGE, TvWallpaperKind.GIF, TvWallpaperKind.BING, TvWallpaperKind.FOCUSED -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(matrix),
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

private fun colorAdjustMatrix(contrast: Float, saturation: Float, hueDeg: Float): ColorMatrix {
    val c = ColorMatrix().apply { setToSaturation(saturation.coerceIn(0f, 2f)) }
    val contrastM = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, (1f - contrast) * 128f,
            0f, contrast, 0f, 0f, (1f - contrast) * 128f,
            0f, 0f, contrast, 0f, (1f - contrast) * 128f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    c.timesAssign(contrastM)
    if (hueDeg != 0f) {
        val shift = (hueDeg.coerceIn(-180f, 180f) / 180f) * 0.15f
        val hue = ColorMatrix(
            floatArrayOf(
                1f, shift, 0f, 0f, 0f,
                0f, 1f, shift, 0f, 0f,
                shift, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        c.timesAssign(hue)
    }
    return c
}

@Composable
private fun TvVideoWallpaper(uri: Uri, zoom: Float = 1f) {
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
            view.scaleX = zoom.coerceIn(0.8f, 1.4f)
            view.scaleY = zoom.coerceIn(0.8f, 1.4f)
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
    val spacing = state.settings.tvCardSpacing.coerceIn(6, 28).dp
    when {
        row.isActions -> TvActionRow(state, viewModel)
        row.isLetters -> TvRow(title = row.def.displayTitle, spacing = spacing) {
            ('A'..'Z').forEach { letter ->
                TvFocusCard(letter.toString(), state, viewModel, onClick = {
                    viewModel.open(Overlay.Search)
                    viewModel.onSearch(letter.toString())
                })
            }
        }
        row.inputs.isNotEmpty() -> TvRow(title = row.def.displayTitle, spacing = spacing) {
            row.inputs.forEach { source ->
                TvFocusCard(label = source.label, state = state, viewModel = viewModel, onClick = {
                    viewModel.launchInput(source)
                }, onLongClick = { viewModel.toggleBlockedInput(source.id) })
            }
        }
        row.watchItems.isNotEmpty() -> TvRow(title = row.def.displayTitle, spacing = spacing) {
            row.watchItems.forEach { item ->
                TvWatchCard(item, row.def.id, state, viewModel)
            }
        }
        else -> TvRow(title = row.def.displayTitle, spacing = spacing) {
            row.apps.forEach { app ->
                TvAppCard(app, row.def.id, state, viewModel)
            }
        }
    }
}

@Composable
private fun TvActionRow(state: HomeUiState, viewModel: HomeViewModel) {
    val spacing = state.settings.tvCardSpacing.coerceIn(6, 28).dp
    TvRow(title = "Hızlı erişim", spacing = spacing) {
        TvFocusCard("Tüm uygulamalar", state, viewModel, onClick = { viewModel.open(Overlay.AllApps) })
        TvFocusCard("Çekmece", state, viewModel, onClick = { viewModel.open(Overlay.Drawer) })
        TvFocusCard("Ara", state, viewModel, onClick = { viewModel.open(Overlay.Search) })
        if (!state.settings.tvHideSettingsChip) {
            TvFocusCard("Ayarlar", state, viewModel, onClick = { viewModel.open(Overlay.Settings) })
        }
        TvFocusCard("Satırlar", state, viewModel, onClick = { viewModel.open(Overlay.TvRows) })
        TvFocusCard("APK kur", state, viewModel, onClick = { viewModel.open(Overlay.Sideload) })
        TvFocusCard("Ekran koruyucu", state, viewModel, onClick = { viewModel.openDreamSettings() })
        TvFocusCard("Duvar kağıdı", state, viewModel, onClick = { viewModel.openWallpaperPicker() })
        if (!state.isDefaultLauncher) {
            TvFocusCard("Varsayılan yap", state, viewModel, onClick = { viewModel.openDefaultHomePicker() })
        }
    }
}

@Composable
private fun TvRow(title: String, spacing: androidx.compose.ui.unit.Dp = 14.dp, content: @Composable RowScope.() -> Unit) {
    Column {
        Text(title, color = Paper, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            content()
        }
    }
}

@Composable
private fun TvWatchCard(item: WatchNextItem, rowId: String, state: HomeUiState, viewModel: HomeViewModel) {
    val showBar = state.settings.tvWatchProgressBar
    val preview = item.previewUri
    val showPreview = state.settings.tvVideoPreviewOnFocus && preview.isNotBlank()
    var focused by remember { mutableStateOf(false) }
    TvFocusCard(
        label = state.tvOverrides[item.app.key]?.customLabel?.ifBlank { item.app.label } ?: item.app.label,
        state = state,
        viewModel = viewModel,
        onClick = { viewModel.launch(item.app) },
        onLongClick = { viewModel.openProgramDetail(item.app) },
        subtitle = item.subtitle,
        progress = if (showBar) item.progress else 0f,
        onFocused = {
            focused = it
            if (it) viewModel.rememberTvCardFocus(rowId, item.app.key)
        },
    ) {
        if (showPreview && focused) {
            AsyncImage(
                model = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.height(8.dp))
        } else {
            HomeApp(item.app, state, viewModel, showLabel = false, iconSize = state.settings.tvCardSize.iconDp.dp)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TvAppCard(
    app: AppInfo,
    rowId: String,
    state: HomeUiState,
    viewModel: HomeViewModel,
) {
    val ov = state.tvOverrides[app.key]
    val label = ov?.customLabel?.ifBlank { app.label } ?: app.label
    TvFocusCard(
        label = label,
        state = state,
        viewModel = viewModel,
        onClick = { viewModel.launch(app) },
        onLongClick = { viewModel.openShutter(app) },
        onFocused = { if (it) viewModel.rememberTvCardFocus(rowId, app.key) },
    ) {
        if (!ov?.customIconUri.isNullOrBlank()) {
            AsyncImage(
                model = ov?.customIconUri,
                contentDescription = null,
                modifier = Modifier.width(state.settings.tvCardSize.iconDp.dp).height(state.settings.tvCardSize.iconDp.dp),
            )
            Spacer(Modifier.height(8.dp))
        } else {
            HomeApp(app, state, viewModel, showLabel = false, iconSize = state.settings.tvCardSize.iconDp.dp)
            Spacer(Modifier.height(8.dp))
        }
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
    onFocused: (Boolean) -> Unit = {},
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
                onFocused(it.isFocused)
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
