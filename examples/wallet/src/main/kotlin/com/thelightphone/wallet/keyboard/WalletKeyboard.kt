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

/**
 * A fully self-contained Compose keyboard for the highest-stakes text entry in the app (private
 * key import today, a PIN later).
 *
 * It never touches the system IME: there is no [android.inputmethodservice.InputMethodService]
 * anywhere in this component, no manifest `<service>` declaration (this SDK's manifest generator
 * deliberately can't declare one), and no keystroke ever leaves the Compose tree - every press is
 * delivered directly through [onKeyPress] / [onBackspace] as a plain callback, in-process.
 *
 * Supports all four layouts LightOS's own system keyboard ships - [KeyboardLayout.EN_QWERTY],
 * [KeyboardLayout.EN_COLEMAK], [KeyboardLayout.FR_AZERTY], [KeyboardLayout.BE_AZERTY] - switchable
 * live via the layout key, with real key-arrangement data ported from `light-keyboard`
 * (see [KeyboardLayoutData] for provenance). This is deliberate: forcing someone into an
 * unfamiliar layout on the input where a mistake is most costly increases mistyping risk, it
 * doesn't reduce it.
 *
 * This composable has no screen to plug into yet - private-key-import and PIN entry screens are
 * a later pass - so it is entirely self-contained and side-effect free: callers own the text
 * buffer, this only reports keystrokes.
 *
 * @param onKeyPress invoked with the literal character to insert (already cased/localized for
 *   the active layout + shift state), including `' '` for the space bar.
 * @param onBackspace invoked when the delete key is pressed.
 * @param initialLayout the layout to render on first composition.
 * @param onLayoutChange invoked whenever the user switches layouts, so a caller can persist the
 *   choice (e.g. to reuse across a private-key-import and a later PIN screen).
 * @param onSubmit if non-null, renders a "done" key that invokes it; otherwise omitted.
 */
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
        // Standard shift behavior: one uppercase key, then back to lowercase. Caps lock
        // (double-tap) is sticky and unaffected by ordinary key presses.
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
