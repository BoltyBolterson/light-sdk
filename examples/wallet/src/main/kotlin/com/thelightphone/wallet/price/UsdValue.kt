package com.thelightphone.wallet.price

import com.thelightphone.wallet.SolanaAmount
import java.math.BigDecimal
import java.math.RoundingMode

/** Cents are a rounding of an exact lamport amount, so callers must mark the result as approximate. */
internal fun solUsdValue(lamports: Long, usdPrice: BigDecimal): BigDecimal =
    BigDecimal(lamports)
        .divide(BigDecimal(SolanaAmount.LAMPORTS_PER_SOL))
        .multiply(usdPrice)
        .setScale(2, RoundingMode.HALF_UP)

internal fun solUsdDisplay(lamports: Long, usdPrice: BigDecimal): String =
    "≈ $${solUsdValue(lamports, usdPrice).toPlainString()}"
