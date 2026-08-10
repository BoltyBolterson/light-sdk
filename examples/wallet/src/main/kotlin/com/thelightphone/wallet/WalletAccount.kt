package com.thelightphone.wallet

data class WalletAccount(
    val chain: Chain,
    val address: String,
    val balance: WalletBalance = WalletBalance.Unread,
)
