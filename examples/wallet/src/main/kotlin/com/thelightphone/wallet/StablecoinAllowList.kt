package com.thelightphone.wallet

data class AllowedStablecoin(val mintAddress: String, val symbol: String, val decimals: Int)

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

    fun isAllowed(mintAddress: String): Boolean = allowList.any { it.mintAddress == mintAddress }
}
