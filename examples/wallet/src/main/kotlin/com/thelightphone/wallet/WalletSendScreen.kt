package com.thelightphone.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.wallet.settings.WalletPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SIGNATURE_CHUNK_SIZE = 8
private const val SOL_DECIMALS = 9

internal sealed interface SendAsset {
    val symbol: String
    val decimals: Int

    object Sol : SendAsset {
        override val symbol = Chain.SOLANA.ticker
        override val decimals = SOL_DECIMALS
    }

    data class Token(val token: AllowedStablecoin) : SendAsset {
        override val symbol get() = token.symbol
        override val decimals get() = token.decimals
    }
}

private val SEND_ASSETS: List<SendAsset> =
    listOf(SendAsset.Sol) + SolanaStablecoins.allowList.map(SendAsset::Token)

private fun SendAsset.baseUnits(input: String): Long? = when (this) {
    SendAsset.Sol -> SolanaAmount.toLamports(input)
    is SendAsset.Token -> token.toBaseUnits(input)
}

private sealed interface SendOutcome {
    data class Sent(val signature: String) : SendOutcome

    data class Failed(val message: String) : SendOutcome
}

class WalletSendScreen(
    sealedActivity: SealedLightActivity,
    private val initialDestination: String = "",
    private val walletId: Long,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = WalletStore.accounts(lightContext)

    @Composable
    override fun Content() {
        var destination by remember { mutableStateOf(initialDestination) }
        var amount by remember { mutableStateOf("") }
        var asset by remember { mutableStateOf<SendAsset>(SendAsset.Sol) }
        var outcome by remember { mutableStateOf<SendOutcome?>(null) }
        var sending by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        WalletScaffold(
            title = "Send",
            onBack = { goBack(Unit) },
            bottomBarItems = listOf(
                LightBarButton.Text(
                    text = if (sending) "SENDING…" else "SEND",
                    onClick = {
                        if (sending) return@Text
                        val recipient = AddressValidator.validate(destination, Chain.SOLANA)
                        if (recipient == null) {
                            outcome = SendOutcome.Failed(
                                "Enter a valid ${Chain.SOLANA.displayName} address before sending.",
                            )
                            return@Text
                        }
                        val chosen = asset
                        val sendAmount = amount
                        val units = chosen.baseUnits(sendAmount)
                        if (units == null) {
                            outcome = SendOutcome.Failed(
                                "Enter a valid ${chosen.symbol} amount with at most " +
                                    "${chosen.decimals} decimal places before sending.",
                            )
                            return@Text
                        }
                        navigateTo(
                            screenFactory = {
                                WalletConfirmSendScreen(it, recipient, sendAmount, chosen.symbol)
                            },
                            resultCallback = { confirmed ->
                                if (!confirmed) return@navigateTo
                                sending = true
                                scope.launch {
                                    val sent = withContext(Dispatchers.IO) {
                                        val rpcUrl =
                                            WalletPreferences.solanaRpcUrl(lightContext.dataStore)
                                                ?: return@withContext Result.failure(
                                                    IllegalStateException(
                                                        "Set a Solana RPC endpoint in Settings before sending.",
                                                    ),
                                                )
                                        runCatching {
                                            when (chosen) {
                                                SendAsset.Sol -> repository.sendSol(
                                                    walletId,
                                                    rpcUrl,
                                                    recipient,
                                                    units,
                                                )

                                                is SendAsset.Token -> repository.sendToken(
                                                    walletId,
                                                    rpcUrl,
                                                    chosen.token.mintAddress,
                                                    recipient,
                                                    sendAmount,
                                                ).signatureOrThrow(chosen.symbol)
                                            }
                                        }
                                    }
                                    sending = false
                                    outcome = sent.fold(
                                        onSuccess = {
                                            amount = ""
                                            SendOutcome.Sent(it)
                                        },
                                        onFailure = {
                                            SendOutcome.Failed(
                                                it.message
                                                    ?: "The network refused the transaction.",
                                            )
                                        },
                                    )
                                }
                            },
                        )
                    },
                ),
            ),
            overlay = {
                when (val current = outcome) {
                    is SendOutcome.Sent -> SentPanel(
                        signature = current.signature,
                        onDone = {
                            outcome = null
                            goBack(Unit)
                        },
                    )

                    is SendOutcome.Failed -> LightFullscreenModal(
                        message = current.message,
                        onClose = { outcome = null },
                    )

                    null -> Unit
                }
            },
        ) {
            EditableField(
                label = "To:",
                title = "To",
                value = destination,
                placeholder = "Destination address",
                onValue = { typed ->
                    val validated = AddressValidator.validate(typed, Chain.SOLANA)
                    if (validated == null) {
                        outcome = SendOutcome.Failed(
                            "That isn't a valid ${Chain.SOLANA.displayName} address.",
                        )
                    } else {
                        destination = validated
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1f.gridUnitsAsDp()),
            )

            LightText(
                text = "SCAN A CODE INSTEAD",
                variant = LightTextVariant.Detail,
                modifier = Modifier
                    .fillMaxWidth()
                    .lightClickable {
                        navigateTo(
                            screenFactory = { WalletScanScreen(it, listOf(Chain.SOLANA)) },
                            resultCallback = { scanned -> destination = scanned },
                        )
                    }
                    .padding(horizontal = 1f.gridUnitsAsDp())
                    .padding(top = 0.5f.gridUnitsAsDp()),
            )

            LightText(
                text = "Asset:",
                variant = LightTextVariant.Detail,
                modifier = Modifier
                    .padding(top = 1f.gridUnitsAsDp())
                    .padding(horizontal = 1f.gridUnitsAsDp()),
            )

            SEND_ASSETS.forEach { candidate ->
                AssetRow(
                    asset = candidate,
                    selected = candidate == asset,
                    onSelect = { asset = candidate },
                    modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                )
            }

            EditableField(
                label = "Amount (${asset.symbol}):",
                title = "Amount",
                value = amount,
                placeholder = "0.00",
                onValue = { amount = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1f.gridUnitsAsDp()),
            )
        }
    }
}

@Composable
private fun AssetRow(
    asset: SendAsset,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onSelect)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(
            text = asset.symbol,
            variant = LightTextVariant.Copy,
            modifier = Modifier.weight(1f),
        )
        LightIcon(
            icon = if (selected) LightIcons.SELECT_ON else LightIcons.SELECT_OFF,
            contentDescription = "${asset.symbol}: ${if (selected) "selected" else "not selected"}",
        )
    }
}

@Composable
private fun SentPanel(signature: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                LightText(
                    text = "Sent",
                    variant = LightTextVariant.Copy,
                    align = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1f.gridUnitsAsDp()),
                )
                LightText(
                    text = "Signature",
                    variant = LightTextVariant.Detail,
                    align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                LightText(
                    text = signature.chunked(SIGNATURE_CHUNK_SIZE).joinToString(" "),
                    variant = LightTextVariant.Detail,
                    align = TextAlign.Center,
                    monospace = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        LightBottomBar(
            items = listOf(
                LightBarButton.Text(
                    text = "DONE",
                    onClick = onDone,
                ),
            ),
        )
    }
}
