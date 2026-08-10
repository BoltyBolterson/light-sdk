package com.thelightphone.wallet

import java.math.BigDecimal

internal object SolanaAmount {
    const val LAMPORTS_PER_SOL = 1_000_000_000L

    fun toLamports(input: String): Long? {
        val decimal = runCatching { BigDecimal(input.trim()) }.getOrNull() ?: return null
        if (decimal.signum() <= 0) return null
        if (decimal.scale() > 9) return null
        val lamports = decimal.multiply(BigDecimal(LAMPORTS_PER_SOL))
        if (lamports.stripTrailingZeros().scale() > 0) return null
        return runCatching { lamports.longValueExact() }.getOrNull()
    }

    fun format(lamports: Long): String =
        BigDecimal(lamports)
            .divide(BigDecimal(LAMPORTS_PER_SOL))
            .stripTrailingZeros()
            .toPlainString()
}
