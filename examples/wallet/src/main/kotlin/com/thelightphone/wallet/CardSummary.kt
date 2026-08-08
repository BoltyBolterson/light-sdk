package com.thelightphone.wallet

/** Just enough to render the Cards list - no payload, matching the "don't decrypt until
 * actually needed" pattern (see WalletSummary for the wallet-side equivalent). */
data class CardSummary(
    val id: Long,
    val name: String,
    val barcodeFormat: String,
)
