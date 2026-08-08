package com.thelightphone.wallet.keyboard

enum class KeyboardLayout(val label: String) {
    EN_QWERTY("QWERTY"),
    EN_COLEMAK("Colemak"),
    FR_AZERTY("AZERTY FR"),
    BE_AZERTY("AZERTY BE"),
    ;

    fun next(): KeyboardLayout = entries[(ordinal + 1) % entries.size]
}

enum class KeyCase {
    LOWER,
    SHIFT,
    CAPS_LOCK,
}

internal data class KeyboardRows(
    val topRow: String,
    val homeRow: String,
    val bottomRow: String,
)

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
