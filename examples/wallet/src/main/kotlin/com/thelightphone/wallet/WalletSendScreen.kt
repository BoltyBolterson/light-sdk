package com.thelightphone.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.wallet.settings.WalletPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletSendScreen(
    sealedActivity: SealedLightActivity,
    private val destinationAddress: String,
    private val walletId: Long,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = WalletStore.accounts(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var amount by remember { mutableStateOf("") }
        var errorModal by remember { mutableStateOf<String?>(null) }
        var sending by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack(Unit) },
                    ),
                    center = LightTopBarCenter.Text("Send"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightTextField(
                    label = "To:",
                    value = destinationAddress,
                    placeholder = "Destination address",
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                )

                LightTextField(
                    label = "Amount:",
                    value = amount,
                    placeholder = "0.00",
                    onClick = {
                        navigateTo(
                            screenFactory = {
                                WalletTextEditorScreen(
                                    it,
                                    WalletEditorRequest(title = "Amount", initialValue = amount),
                                )
                            },
                            resultCallback = { amount = it },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                )

                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text(
                            text = if (sending) "SENDING…" else "SEND",
                            onClick = {
                                if (sending) return@Text
                                val lamports = SolanaAmount.toLamports(amount)
                                if (lamports == null) {
                                    errorModal = "Enter a valid amount before sending."
                                    return@Text
                                }
                                navigateTo(
                                    screenFactory = {
                                        WalletConfirmSendScreen(it, destinationAddress, amount)
                                    },
                                    resultCallback = { confirmed ->
                                        if (!confirmed) return@navigateTo
                                        sending = true
                                        scope.launch {
                                            val outcome = withContext(Dispatchers.IO) {
                                                val rpcUrl =
                                                    WalletPreferences.solanaRpcUrl(lightContext.dataStore)
                                                        ?: return@withContext Result.failure(
                                                            IllegalStateException(
                                                                "Set a Solana RPC endpoint in Settings before sending.",
                                                            ),
                                                        )
                                                runCatching {
                                                    repository.sendSol(
                                                        walletId,
                                                        rpcUrl,
                                                        destinationAddress,
                                                        lamports,
                                                    )
                                                }
                                            }
                                            sending = false
                                            outcome
                                                .onSuccess { signature ->
                                                    errorModal = "Sent." + "\n\n" + signature
                                                    amount = ""
                                                }
                                                .onFailure { failure ->
                                                    errorModal = failure.message
                                                        ?: "The network refused the transaction."
                                                }
                                        }
                                    },
                                )
                            },
                        ),
                    ),
                )
            }
        }

        errorModal?.let { message ->
            LightFullscreenModal(
                message = message,
                onClose = { errorModal = null },
            )
        }
    }
}
