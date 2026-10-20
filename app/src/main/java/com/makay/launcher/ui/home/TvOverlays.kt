package com.makay.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.TvCalibrationPattern
import com.makay.launcher.domain.TvRowSort
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.DpadAction
import com.makay.launcher.ui.components.DpadChip
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper
import kotlinx.coroutines.delay

@Composable
fun TvIdleOverlay(viewModel: HomeViewModel) {
    var clock by remember {
        mutableStateOf(java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(java.util.Date()))
    }
    var date by remember {
        mutableStateOf(
            java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale("tr"))
                .format(java.util.Date())
                .replaceFirstChar { it.uppercase() },
        )
    }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.util.Date()
            clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(now)
            date = java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale("tr"))
                .format(now).replaceFirstChar { it.uppercase() }
            delay(15_000)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1613))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(clock, color = Paper, fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Text(date, color = Color(0xFFC6A15B), fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            Text("Makay Launcher", color = Color(0xFF8FBFB0), fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            DpadAction("Devam et", onClick = viewModel::dismissIdle)
        }
    }
}

@Composable
fun ParentalPinOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    GlassPanel(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
        Column {
            Text("Ebeveyn kilidi", color = Paper, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Bu içerik kısıtlı. PIN girin.",
                color = Paper.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            BasicTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter { ch -> ch.isDigit() }.take(8)
                    error = false
                },
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(color = Paper, fontSize = 22.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassStrong)
                    .padding(12.dp),
            )
            if (error) {
                Text("PIN hatalı", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DpadAction("Onayla") {
                    if (!viewModel.submitParentalPin(pin)) error = true
                }
                DpadAction("İptal", onClick = viewModel::closeOverlays)
            }
        }
    }
}

@Composable
fun CalibrationOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.calibrationPattern) {
            TvCalibrationPattern.COLOR_BARS -> ColorBars()
            TvCalibrationPattern.GRID -> GridPattern()
            TvCalibrationPattern.GRAYSCALE -> GrayscaleBars()
            TvCalibrationPattern.JUDDER -> JudderPattern()
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp),
        ) {
            Text("Kalibrasyon", color = Paper, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                TvCalibrationPattern.entries.forEach { pattern ->
                    DpadChip(
                        label = when (pattern) {
                            TvCalibrationPattern.COLOR_BARS -> "Renk"
                            TvCalibrationPattern.GRID -> "Izgara"
                            TvCalibrationPattern.GRAYSCALE -> "Gri"
                            TvCalibrationPattern.JUDDER -> "Judder"
                        },
                        selected = state.calibrationPattern == pattern,
                    ) { viewModel.setCalibrationPattern(pattern) }
                }
            }
            DpadAction("Kapat", onClick = viewModel::closeOverlays)
        }
    }
}

@Composable
private fun ColorBars() {
    Row(modifier = Modifier.fillMaxSize()) {
        listOf(
            Color.White, Color.Yellow, Color.Cyan, Color.Green,
            Color.Magenta, Color.Red, Color.Blue, Color.Black,
        ).forEach { color ->
            Box(modifier = Modifier.weight(1f).fillMaxSize().background(color))
        }
    }
}

@Composable
private fun GrayscaleBars() {
    Row(modifier = Modifier.fillMaxSize()) {
        (0..10).forEach { step ->
            val g = step / 10f
            Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(g, g, g)))
        }
    }
}

@Composable
private fun GridPattern() {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val step = size.minDimension / 12f
        var x = 0f
        while (x <= size.width) {
            drawLine(Color.White.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(Color.White.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
            y += step
        }
    }
}

@Composable
private fun JudderPattern() {
    var offset by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            offset = (offset + 8f) % 120f
            delay(16)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        var x = -120f + offset
        while (x < size.width) {
            drawRect(Color.White, topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size(40f, size.height))
            x += 120f
        }
    }
}

@Composable
fun TvRowsEditor(state: HomeUiState, viewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassStrong)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TV satırları", color = Paper, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            DpadAction("Kapat", onClick = viewModel::closeOverlays)
        }
        Text(
            "Sıra, görünürlük, sıralama ve varsayılan satır. Özel satır favorilerden oluşturulur.",
            color = Paper.copy(alpha = 0.65f),
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        DpadAction("Favorilerden özel satır ekle") {
            viewModel.addCustomTvRowFromFavorites()
        }
        DpadAction("Varsayılan düzene sıfırla") {
            viewModel.resetTvRows()
        }
        Spacer(Modifier.height(12.dp))
        state.tvRows.forEach { row ->
            GlassPanel(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column {
                    Text(row.displayTitle, color = Paper, fontWeight = FontWeight.Bold)
                    Text(
                        if (row.builtin != null) "Yerleşik · ${row.builtin.name}" else "Özel · ${row.appKeys.size} uygulama",
                        color = Paper.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                        DpadChip(if (row.visible) "Gizli" else "Göster", selected = !row.visible) {
                            viewModel.updateTvRow(row.id) { it.copy(visible = !it.visible) }
                        }
                        DpadChip("Varsayılan", selected = row.isDefault) {
                            viewModel.setDefaultTvRow(row.id)
                        }
                        DpadAction("↑") { viewModel.moveTvRow(row.id, -1) }
                        DpadAction("↓") { viewModel.moveTvRow(row.id, 1) }
                        if (row.builtin == null) {
                            DpadAction("Sil") { viewModel.removeTvRow(row.id) }
                        }
                    }
                    Text("Sıralama", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TvRowSort.entries.forEach { sort ->
                            DpadChip(
                                label = when (sort) {
                                    TvRowSort.ALPHA -> "A–Z"
                                    TvRowSort.LAST_USED -> "Son"
                                    TvRowSort.USAGE -> "Sık"
                                    TvRowSort.RANDOM -> "Rastgele"
                                    TvRowSort.MANUAL -> "Elle"
                                },
                                selected = row.sort == sort,
                            ) { viewModel.updateTvRow(row.id) { it.copy(sort = sort) } }
                        }
                    }
                    Text("Max öğe (0=sınırsız)", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 6, 10, 14, 20).forEach { count ->
                            DpadChip("$count", selected = row.maxItems == count) {
                                viewModel.updateTvRow(row.id) { it.copy(maxItems = count) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
