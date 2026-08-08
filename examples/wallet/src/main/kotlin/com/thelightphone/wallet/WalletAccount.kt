package com.thelightphone.wallet

data class WalletAccount(
    val chain: Chain,
    val address: String,
    // Balance fetching isn't wired up yet - no chain RPC client exists in this scaffold.
    val balanceDisplay: String = "—",
)
