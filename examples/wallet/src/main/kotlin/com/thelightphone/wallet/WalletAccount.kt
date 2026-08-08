package com.thelightphone.wallet

data class WalletAccount(
    val chain: Chain,
    val address: String,
    val balanceDisplay: String = "—",
)
