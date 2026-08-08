package com.thelightphone.wallet

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.LightQrCodeScanner
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens

/**
 * Scans a destination address for the send flow. Returns the raw scanned string -
 * parsing chain-specific URI formats (e.g. a Solana Pay request) isn't done yet, so unlike
 * AuthenticatorQrScannerScreen's Result-wrapped return (which reflects a real parse step that
 * can fail), there's no failure case to model here yet.
 */
class WalletScanScreen(
    sealedActivity: SealedLightActivity,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var pendingScan by remember { mutableStateOf<String?>(null) }
        LightTheme(colors = themeColors) {
            LightQrCodeScanner(
                title = "Scan address",
                onScanned = { pendingScan = it },
                onBack = { goBack() },
                modifier = Modifier.background(LightThemeTokens.colors.background),
            )
        }

        LaunchedEffect(pendingScan) {
            val value = pendingScan ?: return@LaunchedEffect
            goBack(value)
        }
    }
}
