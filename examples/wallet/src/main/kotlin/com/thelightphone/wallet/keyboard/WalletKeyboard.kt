package com.thelightphone.wallet.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens

private val KEY_HEIGHT = 46.dp
private val KEY_CORNER_RADIUS = 6.dp
private val KEY_SPACING = 4.dp
private val ROW_SPACING = 8.dp

@Composable
fun WalletKeyboard(
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    initialLayout: KeyboardLayout = KeyboardLayout.EN_QWERTY,
    onLayoutChange: (KeyboardLayout) -> Unit = {},
    onSubmit: (() -> Unit)? = null,
) {
    var layout by remember { mutableStateOf(initialLayout) }
    var case by remember { mutableStateOf(KeyCase.LOWER) }
    val rows = remember(layout, case) { KeyboardLayoutData.rows(layout, case) }
    val colors = LightThemeTokens.colors

    fun pressLetter(c: Char) {
        onKeyPress(c)
        if (case == KeyCase.SHIFT) case = KeyCase.LOWER
    }

    fun cycleLayout() {
        layout = layout.next()
        case = KeyCase.LOWER
        onLayoutChange(layout)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(top = 8.dp, bottom = 12.dp, start = 6.dp, end = 6.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        KeyRow {
            for (c in rows.topRow) {
                LetterKey(char = c, modifier = Modifier.weight(1f), onPress = ::pressLetter)
            }
        }
        KeyRow {
            for (c in rows.homeRow) {
                LetterKey(char = c, modifier = Modifier.weight(1f), onPress = ::pressLetter)
            }
        }
        KeyRow {
            ShiftKey(
                case = case,
                modifier = Modifier.weight(1.5f),
                onTap = {
                    case = when (case) {
                        KeyCase.LOWER -> KeyCase.SHIFT
                        KeyCase.SHIFT -> KeyCase.LOWER
                        KeyCase.CAPS_LOCK -> KeyCase.LOWER
                    }
                },
                onDoubleTap = { case = KeyCase.CAPS_LOCK },
            )
            for (c in rows.bottomRow) {
                LetterKey(char = c, modifier = Modifier.weight(1f), onPress = ::pressLetter)
            }
            ControlKey(
                label = "DEL",
                modifier = Modifier.weight(1.5f),
                onTap = onBackspace,
            )
        }
        KeyRow {
            ControlKey(
                label = layout.label,
                modifier = Modifier.weight(1.8f),
                onTap = ::cycleLayout,
            )
            ControlKey(
                label = "space",
                modifier = Modifier.weight(3.4f),
                onTap = { onKeyPress(' ') },
            )
            if (onSubmit != null) {
                ControlKey(
                    label = "done",
                    modifier = Modifier.weight(1.8f),
                    onTap = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(KEY_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
        content = content,
    )
}

@Composable
private fun RowScope.LetterKey(
    char: Char,
    modifier: Modifier,
    onPress: (Char) -> Unit,
) {
    val colors = LightThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(KEY_CORNER_RADIUS))
            .background(colors.contentSecondary.copy(alpha = 0.10f))
            .clickable { onPress(char) },
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = char.toString(), variant = LightTextVariant.Copy)
    }
}

@Composable
private fun RowScope.ControlKey(
    label: String,
    modifier: Modifier,
    onTap: () -> Unit,
) {
    val colors = LightThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(KEY_CORNER_RADIUS))
            .background(colors.contentSecondary.copy(alpha = 0.16f))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Detail,
            align = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.ShiftKey(
    case: KeyCase,
    modifier: Modifier,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val colors = LightThemeTokens.colors
    val active = case != KeyCase.LOWER
    val label = if (case == KeyCase.CAPS_LOCK) "CAPS" else "SHIFT"
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(KEY_CORNER_RADIUS))
            .background(
                if (active) colors.content.copy(alpha = 0.85f)
                else colors.contentSecondary.copy(alpha = 0.16f)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Detail,
            align = TextAlign.Center,
            maxLines = 1,
            color = if (active) colors.background else null,
        )
    }
}
