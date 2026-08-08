package com.thelightphone.wallet.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeTokens

@Composable
private fun WalletKeyboardDemo(startingLayout: KeyboardLayout = KeyboardLayout.EN_QWERTY) {
    var typed by remember { mutableStateOf("") }
    LightTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightThemeTokens.colors.background),
        ) {
            LightText(
                text = typed.ifEmpty { " " },
                variant = LightTextVariant.Heading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                maxLines = 3,
            )
            WalletKeyboard(
                onKeyPress = { typed = typed + it },
                onBackspace = { typed = typed.dropLast(1) },
                initialLayout = startingLayout,
                onSubmit = {},
            )
        }
    }
}

@Preview(name = "EN QWERTY", widthDp = 360, heightDp = 420)
@Composable
private fun WalletKeyboardEnQwertyPreview() {
    WalletKeyboardDemo(KeyboardLayout.EN_QWERTY)
}

@Preview(name = "EN Colemak", widthDp = 360, heightDp = 420)
@Composable
private fun WalletKeyboardEnColemakPreview() {
    WalletKeyboardDemo(KeyboardLayout.EN_COLEMAK)
}

@Preview(name = "FR AZERTY", widthDp = 360, heightDp = 420)
@Composable
private fun WalletKeyboardFrAzertyPreview() {
    WalletKeyboardDemo(KeyboardLayout.FR_AZERTY)
}

@Preview(name = "BE AZERTY", widthDp = 360, heightDp = 420)
@Composable
private fun WalletKeyboardBeAzertyPreview() {
    WalletKeyboardDemo(KeyboardLayout.BE_AZERTY)
}
