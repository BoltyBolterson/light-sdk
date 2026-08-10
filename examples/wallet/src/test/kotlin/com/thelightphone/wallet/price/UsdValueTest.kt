package com.thelightphone.wallet.price

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class UsdValueTest {

    @Test
    fun oneSolIsWorthThePrice() {
        assertEquals(BigDecimal("150.00"), solUsdValue(1_000_000_000L, BigDecimal("150.00")))
    }

    @Test
    fun zeroLamportsIsZeroDollars() {
        assertEquals(BigDecimal("0.00"), solUsdValue(0L, BigDecimal("150.00")))
    }

    @Test
    fun centsRoundHalfUpRatherThanTruncating() {
        assertEquals(BigDecimal("0.02"), solUsdValue(100_000L, BigDecimal("150.00")))
    }

    @Test
    fun aDustBalanceIsNotRoundedUpToACent() {
        assertEquals(BigDecimal("0.00"), solUsdValue(1L, BigDecimal("150.00")))
    }

    @Test
    fun fullPrecisionOfTheFeedIsCarriedIntoTheProduct() {
        assertEquals(BigDecimal("1837.51"), solUsdValue(12_500_000_000L, BigDecimal("147.0009")))
    }

    @Test
    fun theDisplayMarksItselfApproximate() {
        assertEquals("≈ $150.00", solUsdDisplay(1_000_000_000L, BigDecimal("150.00")))
    }
}
