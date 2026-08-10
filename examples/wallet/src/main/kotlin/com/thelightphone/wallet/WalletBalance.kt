package com.thelightphone.wallet

sealed interface WalletBalance {
    object Unsupported : WalletBalance

    object Unread : WalletBalance

    object Unavailable : WalletBalance

    data class Lamports(val value: Long) : WalletBalance
}

internal fun Chain.unreadBalance(): WalletBalance =
    if (this == Chain.SOLANA) WalletBalance.Unread else WalletBalance.Unsupported

/** Null renders nothing at all; only a real read is allowed to look like an amount. */
internal fun WalletBalance.display(chain: Chain): String? = when (this) {
    WalletBalance.Unsupported -> null
    WalletBalance.Unread -> "—"
    WalletBalance.Unavailable -> "unavailable"
    is WalletBalance.Lamports -> "${SolanaAmount.format(value)} ${chain.ticker}"
}

internal fun WalletBalance.isRead(): Boolean = this is WalletBalance.Lamports
