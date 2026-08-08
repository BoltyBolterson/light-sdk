package com.thelightphone.wallet.keyboard

/**
 * The keyboard layouts offered by [WalletKeyboard].
 *
 * These are the same four layouts LightOS's own system keyboard ships (see
 * `lightphone/light-keyboard`, MIT licensed). Forcing a user into an unfamiliar layout on the
 * highest-stakes text entry in this app (a private key import today, a PIN later) increases
 * mistyping risk rather than reducing it - people need to type in whatever layout they normally
 * use. So [WalletKeyboard] must offer all four rather than hard-coding one default.
 */
enum class KeyboardLayout(val label: String) {
    EN_QWERTY("QWERTY"),
    EN_COLEMAK("Colemak"),
    FR_AZERTY("AZERTY FR"),
    BE_AZERTY("AZERTY BE"),
    ;

    /** Cycles to the next layout in the fixed order above, wrapping around. */
    fun next(): KeyboardLayout = entries[(ordinal + 1) % entries.size]
}

/**
 * Shift state, mirroring standard mobile-keyboard shift semantics:
 * [LOWER] is the default; a single shift tap produces one uppercase key then reverts
 * automatically ([SHIFT]); a double tap locks uppercase until toggled off ([CAPS_LOCK]).
 */
enum class KeyCase {
    LOWER,
    SHIFT,
    CAPS_LOCK,
}

/** The three letter rows rendered above the space bar for a given layout + case. */
internal data class KeyboardRows(
    val topRow: String,
    val homeRow: String,
    val bottomRow: String,
)

/**
 * Key-arrangement data for each [KeyboardLayout], ported verbatim (row contents unchanged, not
 * reconstructed) from LightOS's own keyboard project -  `lightphone/light-keyboard` (MIT
 * License) - at tag `v0.0.18`:
 * `ui/src/main/java/com/thelightphone/lp3Keyboard/ui/layout/EnQwerty.kt`
 * `ui/src/main/java/com/thelightphone/lp3Keyboard/ui/layout/EnColemak.kt`
 * `ui/src/main/java/com/thelightphone/lp3Keyboard/ui/layout/FrAzerty.kt`
 * `ui/src/main/java/com/thelightphone/lp3Keyboard/ui/layout/BeAzerty.kt`
 *
 * Note: this SDK currently pins `light-keyboard` at `v0.0.16` (see
 * `gradle/libs.versions.toml`), which predates the FrAzerty/BeAzerty layouts landing upstream
 * (added in `lightphone/light-keyboard#14`, first released in `v0.0.18`) - so their row data is
 * reproduced here directly from the upstream source rather than taken from the resolved
 * dependency jar. [WalletKeyboard] itself does not depend on the `light-keyboard` artifact at
 * all; it renders these rows with its own Compose primitives so it never has to route through
 * that library's `ViewModel`/`IME`-oriented plumbing.
 *
 * One deliberate nuance preserved rather than flattened: Colemak's trailing top-row punctuation
 * key is case-sensitive upstream - apostrophe (`'`) while lowercase or caps-locked, double-quote
 * (`"`) on a single shift-tap only. See the upstream rationale at
 * `github.com/lightphone/light-keyboard/pull/4`.
 */
internal object KeyboardLayoutData {
    fun rows(layout: KeyboardLayout, case: KeyCase): KeyboardRows = when (layout) {
        KeyboardLayout.EN_QWERTY -> when (case) {
            KeyCase.LOWER -> KeyboardRows("qwertyuiop", "asdfghjkl", "zxcvbnm")
            KeyCase.SHIFT, KeyCase.CAPS_LOCK -> KeyboardRows("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
        }

        KeyboardLayout.EN_COLEMAK -> when (case) {
            KeyCase.LOWER -> KeyboardRows("qwfpgjluy'", "arstdhneio", "zxcvbkm")
            KeyCase.CAPS_LOCK -> KeyboardRows("QWFPGJLUY'", "ARSTDHNEIO", "ZXCVBKM")
            KeyCase.SHIFT -> KeyboardRows("QWFPGJLUY\"", "ARSTDHNEIO", "ZXCVBKM")
        }

        KeyboardLayout.FR_AZERTY -> when (case) {
            KeyCase.LOWER -> KeyboardRows("azertyuiop", "qsdfghjklm", "wxcvbn")
            KeyCase.SHIFT, KeyCase.CAPS_LOCK -> KeyboardRows("AZERTYUIOP", "QSDFGHJKLM", "WXCVBN")
        }

        KeyboardLayout.BE_AZERTY -> when (case) {
            KeyCase.LOWER -> KeyboardRows("azertyuiop", "qsdfghjklm", "wxcvbn")
            KeyCase.SHIFT, KeyCase.CAPS_LOCK -> KeyboardRows("AZERTYUIOP", "QSDFGHJKLM", "WXCVBN")
        }
    }
}
