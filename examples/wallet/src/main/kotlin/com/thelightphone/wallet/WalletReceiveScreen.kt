package com.thelightphone.wallet

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp

private const val QR_PLACEHOLDER_WIDTH_FRACTION = 0.58f

class WalletReceiveScreen(
    sealedActivity: SealedLightActivity,
    private val account: WalletAccount,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        WalletScaffold(
            title = "Receive ${account.chain.ticker}",
            onBack = { goBack(Unit) },
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 1f.gridUnitsAsDp()),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2f.gridUnitsAsDp()),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(QR_PLACEHOLDER_WIDTH_FRACTION)
                            .aspectRatio(1f)
                            .border(width = 2.dp, color = LightThemeTokens.colors.content)
                            .padding(1f.gridUnitsAsDp()),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "Not scannable yet\n\nthe address is below",
                            variant = LightTextVariant.Detail,
                            align = TextAlign.Center,
                        )
                    }
                    LightText(
                        text = account.address,
                        variant = LightTextVariant.Detail,
                        align = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
