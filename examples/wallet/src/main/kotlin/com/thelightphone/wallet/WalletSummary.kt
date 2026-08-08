package com.thelightphone.wallet

/** Just enough to render the Wallets list screen. Per-chain addresses for a given wallet are
 * derived on demand via WalletAccountRepository.listAccounts(walletId), not carried here. */
data class WalletSummary(
    val id: Long,
    val name: String,
)
