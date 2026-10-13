@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.makay.launcher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.PinnedWidget
import com.makay.launcher.domain.WorkspaceItem
import com.makay.launcher.domain.WorkspaceType
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.components.DpadChip
import com.makay.launcher.ui.components.dpadClickable
import com.makay.launcher.ui.theme.GlassStrong
import com.makay.launcher.ui.theme.Paper
import kotlin.math.roundToInt

@Composable
fun WorkspaceGrid(state: HomeUiState, viewModel: HomeViewModel) {
    val columns = if (state.isWideScreen) state.settings.gridColumns + 2 else state.settings.gridColumns
    val rows = state.settings.gridRows
    val lastPage = state.workspace.maxOfOrNull { it.page } ?: 0
    val pageCount = (lastPage + 2).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            WorkspacePage(
                page = page,
                columns = columns,
                rows = rows,
                state = state,
                viewModel = viewModel,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else Paper.copy(alpha = 0.35f),
                        ),
                )
            }
            if (state.selectedItemId != null) {
                DpadChip("Daralt", false) { viewModel.resizeSelected(-1, 0) }
                DpadChip("Genişlet", false) { viewModel.resizeSelected(1, 0) }
                DpadChip("Alçalt", false) { viewModel.resizeSelected(0, -1) }
                DpadChip("Yükselt", false) { viewModel.resizeSelected(0, 1) }
                DpadChip("Sil", false) { viewModel.removeWorkspaceItem(state.selectedItemId) }
            }
        }
    }
}

@Composable
private fun WorkspacePage(
    page: Int,
    columns: Int,
    rows: Int,
    state: HomeUiState,
    viewModel: HomeViewModel,
) {
    val items = state.workspace.filter { it.page == page }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cellW = maxWidth / columns
        val cellH = maxHeight / rows
        val density = LocalDensity.current
        items.forEach { item ->
            var drag by remember(item.id) { mutableStateOf(Offset.Zero) }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (cellW.toPx() * item.cellX + drag.x).roundToInt(),
                            (cellH.toPx() * item.cellY + drag.y).roundToInt(),
                        )
                    }
                    .size(cellW * item.spanX, cellH * item.spanY)
                    .then(
                        if (state.selectedItemId == item.id) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        } else Modifier
                    )
                    .pointerInput(item.id, page, columns, rows) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { viewModel.selectItem(item.id) },
                            onDrag = { change, amount ->
                                change.consume()
                                drag += amount
                            },
                            onDragEnd = {
                                val x = item.cellX + (drag.x / with(density) { cellW.toPx() }).roundToInt()
                                val y = item.cellY + (drag.y / with(density) { cellH.toPx() }).roundToInt()
                                viewModel.dropItem(
                                    item.id,
                                    page,
                                    x.coerceIn(0, columns - 1),
                                    y.coerceIn(0, rows - 1),
                                )
                                drag = Offset.Zero
                            },
                            onDragCancel = { drag = Offset.Zero },
                        )
                    },
            ) {
                when (item.type) {
                    WorkspaceType.APP -> {
                        val app = state.apps.find { it.key == item.appKey }
                        if (app != null) {
                            val shortcutId = item.shortcutId
                            HomeApp(
                                app = app,
                                state = state,
                                viewModel = viewModel,
                                showLabel = !state.settings.textOnly,
                                label = item.title.takeIf { shortcutId.isNotBlank() && it.isNotBlank() },
                                onClick = if (shortcutId.isNotBlank()) {
                                    {
                                        viewModel.launchShortcut(
                                            app,
                                            com.makay.launcher.domain.AppShortcut(
                                                shortcutId,
                                                item.title.ifBlank { app.label },
                                                app.packageName,
                                            ),
                                        )
                                    }
                                } else null,
                            )
                        }
                    }
                    WorkspaceType.FOLDER -> FolderCell(item, state, viewModel)
                    WorkspaceType.WIDGET -> {
                        val widget = state.widgets.find { it.appWidgetId == item.widgetId }
                            ?: PinnedWidget(item.widgetId, item.provider)
                        WidgetFrame(
                            widget,
                            onRemove = { viewModel.removeWorkspaceItem(item.id) },
                            onRebind = { viewModel.rebindWidget(item.provider.ifBlank { widget.provider }) },
                            widthDp = (cellW * item.spanX).value.toInt(),
                            heightDp = (cellH * item.spanY).value.toInt(),
                        )
                    }
                }
            }
        }
        if (items.isEmpty()) {
            Text(
                "Boş sayfa · çekmeceden uzun basıp ekleyin veya ikonu buraya sürükleyin",
                color = Paper.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            )
        }
    }
}

@Composable
private fun FolderCell(
    item: WorkspaceItem,
    state: HomeUiState,
    viewModel: HomeViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (item.color != 0) Color(item.color) else GlassStrong)
            .dpadClickable { viewModel.openFolder(item.id) }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📁", fontSize = 18.sp)
            Text(
                item.title.ifBlank { "${item.members.size} uygulama" },
                color = Paper,
                fontSize = 11.sp,
            )
        }
    }
}
