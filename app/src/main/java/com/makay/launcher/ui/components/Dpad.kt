@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.makay.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makay.launcher.ui.theme.Paper

fun Modifier.dpadClickable(
    focusRequester: FocusRequester? = null,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
): Modifier {
    val keys = this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
            when (event.key) {
                Key.Enter, Key.DirectionCenter -> {
                    onClick()
                    true
                }
                Key.Menu -> {
                    onLongClick()
                    true
                }
                else -> false
            }
        }
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .focusable()
    return keys
}

@Composable
fun DpadAction(
    label: String,
    emphasis: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Text(
        text = label,
        color = Paper,
        fontSize = if (emphasis) 17.sp else 15.sp,
        modifier = Modifier
            .clip(shape)
            .background(
                when {
                    focused && emphasis -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    emphasis -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                },
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .dpadClickable(onClick = onClick, focusRequester = focusRequester)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun DpadChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Text(
        text = label,
        color = Paper,
        fontSize = 13.sp,
        modifier = Modifier
            .padding(end = 8.dp, bottom = 6.dp)
            .clip(shape)
            .background(
                when {
                    focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                },
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused || selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .dpadClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
