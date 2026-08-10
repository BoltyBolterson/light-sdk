package com.thelightphone.wallet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val USDC_DECIMALS = 6

class TokenAmountTest {

    @Test
    fun wholeAndFractionalUsdcConverts() {
        assertEquals(1_000_000L, TokenAmount.toBaseUnits("1", USDC_DECIMALS))
        assertEquals(1_500_000L, TokenAmount.toBaseUnits("1.5", USDC_DECIMALS))
        assertEquals(1L, TokenAmount.toBaseUnits("0.000001", USDC_DECIMALS))
        assertEquals(123_456L, TokenAmount.toBaseUnits("0.123456", USDC_DECIMALS))
        assertEquals(1_000_000L, TokenAmount.toBaseUnits(" 1.000000 ", USDC_DECIMALS))
    }

    @Test
    fun moreThanSixDecimalsIsRefusedRatherThanRounded() {
        assertNull(TokenAmount.toBaseUnits("0.0000001", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("1.1234567", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("0.9999999", USDC_DECIMALS))
    }

    @Test
    fun trailingZerosBeyondTheMintsPrecisionAreStillExact() {
        assertEquals(1_500_000L, TokenAmount.toBaseUnits("1.50000000000", USDC_DECIMALS))
    }

    @Test
    fun zeroNegativeAndNonNumericAreRefused() {
        assertNull(TokenAmount.toBaseUnits("0", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("0.000000", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("-1", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("-0.000001", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("1.2.3", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("all of it", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("NaN", USDC_DECIMALS))
    }

    @Test
    fun theLargestRepresentableAmountConvertsAndTheNextOneDoesNot() {
        assertEquals(Long.MAX_VALUE, TokenAmount.toBaseUnits("9223372036854.775807", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("9223372036854.775808", USDC_DECIMALS))
    }

    @Test
    fun anAbsurdExponentIsRefusedWithoutExpandingIt() {
        assertNull(TokenAmount.toBaseUnits("1E+2000000000", USDC_DECIMALS))
        assertNull(TokenAmount.toBaseUnits("1E+20", USDC_DECIMALS))
        assertEquals(1_000_000_000L, TokenAmount.toBaseUnits("1E+3", USDC_DECIMALS))
    }

    @Test
    fun eachMintUsesItsOwnDecimals() {
        assertEquals(1L, TokenAmount.toBaseUnits("1", 0))
        assertNull(TokenAmount.toBaseUnits("0.1", 0))
        assertEquals(1_000_000_000L, TokenAmount.toBaseUnits("1", 9))
        assertNull(TokenAmount.toBaseUnits("1", -1))
        assertNull(TokenAmount.toBaseUnits("1", 19))
    }

    @Test
    fun theAllowListEntryCarriesItsOwnPrecision() {
        assertEquals(1_000_000L, SolanaStablecoins.USDC.toBaseUnits("1"))
        assertNull(SolanaStablecoins.USDC.toBaseUnits("0.0000001"))
        assertEquals(1_000_000L, SolanaStablecoins.USDT.toBaseUnits("1"))
    }

    @Test
    fun formattingRoundTrips() {
        assertEquals("1.5", TokenAmount.format(1_500_000L, USDC_DECIMALS))
        assertEquals("0.000001", TokenAmount.format(1L, USDC_DECIMALS))
        assertEquals("0", TokenAmount.format(0L, USDC_DECIMALS))
        assertEquals("1.5", SolanaStablecoins.USDC.format(1_500_000L))
    }
}
