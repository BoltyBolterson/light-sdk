package com.thelightphone.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens

class WalletScanScreen(
    sealedActivity: SealedLightActivity,
    private val chains: List<Chain>,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var pendingScan by remember { mutableStateOf<String?>(null) }
        var rejectedScan by remember { mutableStateOf(false) }
        LightTheme(colors = themeColors) {
            Box(modifier = Modifier.fillMaxSize()) {
                LightQrCodeScanner(
                    title = "Scan address",
                    onScanned = { scanned ->
                        val address = AddressValidator.validateAny(scanned, chains)
                        if (address == null) rejectedScan = true else pendingScan = address
                    },
                    onBack = { goBack() },
                    modifier = Modifier.background(LightThemeTokens.colors.background),
                )

                if (rejectedScan) {
                    LightFullscreenModal(
                        message = "That code isn't a valid " +
                            "${chains.joinToString(" or ") { it.displayName }} address.",
                        onClose = { rejectedScan = false },
                    )
                }
            }
        }

        LaunchedEffect(pendingScan) {
            val value = pendingScan ?: return@LaunchedEffect
            goBack(value)
        }
    }
}
