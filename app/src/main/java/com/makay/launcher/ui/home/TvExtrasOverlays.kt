package com.makay.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.TvCardIconMode
import com.makay.launcher.domain.TvRowDef
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.DpadAction
import com.makay.launcher.ui.components.DpadChip
import com.makay.launcher.ui.components.GlassPanel
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper

@Composable
fun AllAppsPanel(state: HomeUiState, viewModel: HomeViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000))) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
                .background(GlassStrong)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tüm uygulamalar", color = Paper, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                DpadAction("Kapat", onClick = viewModel::closeOverlays)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.apps.sortedBy { it.label.lowercase() }, key = { it.key }) { app ->
                    val label = state.tvOverrides[app.key]?.customLabel?.ifBlank { app.label } ?: app.label
                    DpadAction(label) { viewModel.launch(app) }
                }
            }
        }
    }
}

@Composable
fun ProgramDetailPanel(state: HomeUiState, viewModel: HomeViewModel) {
    val detail = state.programDetail ?: return
    Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000))) {
        GlassPanel(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.45f)
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (detail.previewUri.isNotBlank()) {
                    AsyncImage(
                        model = detail.previewUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(detail.title, color = Paper, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(detail.subtitle, color = Paper.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
                if (detail.detail.isNotBlank()) {
                    Text(detail.detail, color = Paper.copy(alpha = 0.65f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (detail.progress > 0f) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { detail.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                    Text("%${(detail.progress * 100).toInt()}", color = Paper.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                DpadAction("Aç") { viewModel.launch(detail.app) }
                DpadAction("Kapat", onClick = viewModel::closeOverlays)
            }
        }
    }
}

@Composable
fun SideloadOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    var url by remember { mutableStateOf(state.sideloadUrl) }
    GlassPanel(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
        Column {
            Text("APK URL’den kur", color = Paper, style = MaterialTheme.typography.headlineSmall)
            Text("https://… adresinden APK indirip kurulum intent’i açar.", color = Paper.copy(alpha = 0.65f), fontSize = 12.sp)
            BasicTextField(
                value = url,
                onValueChange = { url = it },
                textStyle = TextStyle(color = Paper, fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DpadAction("İndir ve kur") { viewModel.sideloadFromUrl(url) }
                DpadAction("İptal", onClick = viewModel::closeOverlays)
            }
        }
    }
}

@Composable
fun TvAppEditOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    val app = state.editingTvApp ?: return
    val current = state.tvOverrides[app.key]
    var label by remember { mutableStateOf(current?.customLabel ?: app.label) }
    var preview by remember { mutableStateOf(current?.previewUri.orEmpty()) }
    GlassPanel(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Column {
            Text("Kart özelleştir: ${app.label}", color = Paper, style = MaterialTheme.typography.titleLarge)
            Text("Görünen ad", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            BasicTextField(
                value = label,
                onValueChange = { label = it },
                textStyle = TextStyle(color = Paper, fontSize = 16.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            Text("Önizleme URI (görsel/video)", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp)
            BasicTextField(
                value = preview,
                onValueChange = { preview = it },
                textStyle = TextStyle(color = Paper, fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            Text("İkon / banner", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DpadChip("İkon", current?.iconMode != TvCardIconMode.BANNER) {
                    viewModel.saveTvAppOverride(app, label, preview, TvCardIconMode.ICON)
                }
                DpadChip("Banner", current?.iconMode == TvCardIconMode.BANNER) {
                    viewModel.saveTvAppOverride(app, label, preview, TvCardIconMode.BANNER)
                }
            }
            Spacer(Modifier.height(8.dp))
            DpadAction("Kaydet") {
                viewModel.saveTvAppOverride(app, label, preview, current?.iconMode ?: TvCardIconMode.ICON)
                viewModel.closeOverlays()
            }
            DpadAction("Sıfırla") {
                viewModel.clearTvAppOverride(app)
                viewModel.closeOverlays()
            }
            Text("Özel satıra ekle", color = Paper.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            state.tvRows.filter { it.builtin == null || it.id.startsWith("custom") }.forEach { row: TvRowDef ->
                DpadAction(row.displayTitle) {
                    viewModel.addAppToTvRow(row.id, app.key)
                }
            }
            DpadAction("Yeni özel satıra ekle") {
                viewModel.addAppToNewCustomRow(app)
            }
        }
    }
}

@Composable
fun AutoBackupsOverlay(state: HomeUiState, viewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassStrong)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Otomatik yedekler", color = Paper, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            DpadAction("Kapat", onClick = viewModel::closeOverlays)
        }
        DpadAction("Şimdi yedekle") { viewModel.runAutoBackup(force = true) }
        if (state.autoBackupFiles.isEmpty()) {
            Text("Henüz otomatik yedek yok", color = Paper.copy(alpha = 0.65f), modifier = Modifier.padding(top = 12.dp))
        } else {
            state.autoBackupFiles.forEach { name ->
                DpadAction("Geri yükle · $name") { viewModel.restoreAutoBackup(name) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
