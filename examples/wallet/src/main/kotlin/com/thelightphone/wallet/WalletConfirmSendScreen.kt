package com.thelightphone.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp

private const val ADDRESS_CHUNK_SIZE = 4

class WalletConfirmSendScreen(
    sealedActivity: SealedLightActivity,
    private val destinationAddress: String,
    private val amount: String,
    private val assetSymbol: String,
) : SimpleLightScreen<Boolean>(sealedActivity) {

    @Composable
    override fun Content() {
        WalletScaffold(
            title = "Confirm Send",
            onBack = { goBack(false) },
            bottomBarItems = listOf(
                LightBarButton.Text(
                    text = "CONFIRM",
                    onClick = { goBack(true) },
                ),
            ),
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
                        text = "Amount",
                        variant = LightTextVariant.Detail,
                        align = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LightText(
                        text = "$amount $assetSymbol",
                        variant = LightTextVariant.Copy,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1f.gridUnitsAsDp()),
                    )
                    LightText(
                        text = "Network fee",
                        variant = LightTextVariant.Detail,
                        align = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LightText(
                        text = "${SolanaAmount.format(LAMPORTS_PER_SIGNATURE)} ${Chain.SOLANA.ticker}",
                        variant = LightTextVariant.Detail,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1f.gridUnitsAsDp()),
                    )
                    LightText(
                        text = "To",
                        variant = LightTextVariant.Detail,
                        align = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LightText(
                        text = destinationAddress.chunked(ADDRESS_CHUNK_SIZE).joinToString(" "),
                        variant = LightTextVariant.Detail,
                        align = TextAlign.Center,
                        monospace = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
