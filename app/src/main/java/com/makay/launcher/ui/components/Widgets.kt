@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.makay.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import com.makay.launcher.data.AppIconKey
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.IconShape
import com.makay.launcher.ui.theme.Glass
import com.makay.launcher.ui.theme.Paper
import com.makay.launcher.ui.components.dpadClickable

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (strong) Color(0x99101814) else Glass)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
fun AppIconButton(
    app: AppInfo,
    textOnly: Boolean,
    badge: Int,
    iconSize: Dp = 52.dp,
    showLabel: Boolean = true,
    iconShape: IconShape = IconShape.SQUIRCLE,
    iconPack: String = "",
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .dpadClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!textOnly) {
            Box(contentAlignment = Alignment.TopEnd) {
                val clip = when (iconShape) {
                    IconShape.CIRCLE -> CircleShape
                    IconShape.SQUIRCLE -> RoundedCornerShape(percent = 35)
                    IconShape.TEARDROP -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 36.dp, bottomStart = 20.dp)
                    IconShape.ROUNDED -> RoundedCornerShape(12.dp)
                }
                AsyncImage(
                    model = AppIconKey(app, iconPack, iconShape),
                    contentDescription = when {
                        app.privateSpace -> "${app.label} · özel alan"
                        app.work -> "${app.label} · iş"
                        else -> app.label
                    },
                    modifier = Modifier
                        .size(iconSize)
                        .clip(clip),
                )
                if (badge > 0) {
                    Text(
                        text = if (badge > 9) "9+" else badge.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE24B4A))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                color = Paper,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

fun tileColor(packageName: String): Color {
    val palette = listOf(
        Color(0xFF2F6F6A),
        Color(0xFF8A4B2F),
        Color(0xFF3D5A80),
        Color(0xFF6B4E71),
        Color(0xFF4A7C59),
        Color(0xFFB0892E),
        Color(0xFF7A3E3E),
        Color(0xFF2E4A62),
    )
    return palette[packageName.hashCode().mod(palette.size).let { if (it < 0) it + palette.size else it }]
}
