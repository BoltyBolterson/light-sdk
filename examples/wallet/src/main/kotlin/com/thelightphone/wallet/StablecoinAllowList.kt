package com.thelightphone.wallet

/**
 * A stablecoin the wallet trusts, keyed by its on-chain mint address.
 *
 * [symbol] is display-only. It must never be used to decide whether a token is trusted — Solana
 * lets anyone mint a token with an arbitrary ticker, so a token calling itself "USDC" is not
 * necessarily the real one. Only [mintAddress] is authoritative; see [SolanaStablecoins.isAllowed].
 */
data class AllowedStablecoin(val mintAddress: String, val symbol: String, val decimals: Int)

/**
 * Curated allow-list of Solana SPL stablecoins the wallet recognizes as trusted.
 *
 * This is deliberately narrow: an address-keyed foundation for future SPL-token support, not a
 * general token registry. Any code that decides whether to treat a token as a trusted stablecoin
 * MUST check the mint address via [isAllowed] — never the token's self-reported symbol or name,
 * both of which are attacker-controlled metadata on Solana.
 *
 * Mint addresses verified against primary sources:
 *  - USDC: Circle's own developer docs.
 *  - USDT: Solana's official Tether account.
 * Both confirmed directly on-chain (mainnet-beta getTokenSupply) to have 6 decimals.
 *
 * Note: "USDT0" is a separate, newer omnichain-bridged token with a different mint address. It is
 * NOT the canonical USDT above and is intentionally excluded pending its own deliberate review.
 */
object SolanaStablecoins {
    val USDC = AllowedStablecoin(
        mintAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        symbol = "USDC",
        decimals = 6,
    )

    val USDT = AllowedStablecoin(
        mintAddress = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",
        symbol = "USDT",
        decimals = 6,
    )

    val allowList: List<AllowedStablecoin> = listOf(USDC, USDT)

    /** Returns true only if [mintAddress] exactly matches a curated mint address. Never checks symbol/name. */
    fun isAllowed(mintAddress: String): Boolean = allowList.any { it.mintAddress == mintAddress }
}
