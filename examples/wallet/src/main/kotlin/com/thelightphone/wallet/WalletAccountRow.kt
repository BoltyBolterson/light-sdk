package com.thelightphone.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIconConfiguration
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.wallet.price.solUsdDisplay
import java.math.BigDecimal

private val Chain.icon: LightIconConfiguration
    get() = when (this) {
        Chain.BITCOIN -> LightIcons.BTC
        Chain.ETHEREUM -> LightIcons.ETH
        Chain.SOLANA -> LightIcons.SOL
    }

@Composable
fun WalletAccountRow(
    account: WalletAccount,
    modifier: Modifier = Modifier,
    usdPrice: BigDecimal? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightIcon(
            icon = account.chain.icon,
            size = 1.5f,
            modifier = Modifier.padding(end = 0.5f.gridUnitsAsDp()),
        )
        Column(modifier = Modifier.weight(1f)) {
            LightText(
                text = "${account.chain.displayName} (${account.chain.ticker})",
                variant = LightTextVariant.Copy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LightText(
                text = account.address.truncatedMiddle(),
                variant = LightTextVariant.Detail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        account.balance.display(account.chain)?.let { balance ->
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 0.5f.gridUnitsAsDp(), end = 1f.gridUnitsAsDp()),
            ) {
                LightText(
                    text = balance,
                    variant = LightTextVariant.Copy,
                    align = TextAlign.End,
                    lighten = !account.balance.isRead(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                account.usdLine(usdPrice)?.let {
                    LightText(
                        text = it,
                        variant = LightTextVariant.Detail,
                        align = TextAlign.End,
                        lighten = true,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun WalletAccount.usdLine(usdPrice: BigDecimal?): String? {
    if (chain != Chain.SOLANA || usdPrice == null) return null
    val lamports = (balance as? WalletBalance.Lamports)?.value ?: return null
    return solUsdDisplay(lamports, usdPrice)
}

private fun String.truncatedMiddle(head: Int = 6, tail: Int = 6): String =
    if (length <= head + tail + 3) this else "${take(head)}…${takeLast(tail)}"
