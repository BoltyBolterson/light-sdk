package com.thelightphone.wallet

import java.math.BigDecimal

private const val MAX_INTEGER_DIGITS = 19

data class AllowedStablecoin(val mintAddress: String, val symbol: String, val decimals: Int) {
    fun toBaseUnits(input: String): Long? = TokenAmount.toBaseUnits(input, decimals)

    fun format(baseUnits: Long): String = TokenAmount.format(baseUnits, decimals)
}

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

    fun allowed(mintAddress: String): AllowedStablecoin? = allowList.firstOrNull { it.mintAddress == mintAddress }
}

internal object TokenAmount {
    fun toBaseUnits(input: String, decimals: Int): Long? {
        if (decimals !in 0..18) return null
        val decimal = runCatching { BigDecimal(input.trim()) }.getOrNull()?.stripTrailingZeros() ?: return null
        if (decimal.signum() <= 0) return null
        if (decimal.scale() > decimals) return null
        // Guards longValueExact against materialising 10^huge from an exponent like 1E+2000000000.
        if (decimal.precision() - decimal.scale() > MAX_INTEGER_DIGITS) return null
        return runCatching { decimal.movePointRight(decimals).longValueExact() }.getOrNull()
    }

    fun format(baseUnits: Long, decimals: Int): String =
        BigDecimal(baseUnits).movePointLeft(decimals).stripTrailingZeros().toPlainString()
}
