package com.thelightphone.wallet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SolanaAmountTest {

    @Test
    fun wholeAndFractionalAmountsConvert() {
        assertEquals(1_000_000_000L, SolanaAmount.toLamports("1"))
        assertEquals(1_500_000_000L, SolanaAmount.toLamports("1.5"))
        assertEquals(1L, SolanaAmount.toLamports("0.000000001"))
        assertEquals(123_456_789L, SolanaAmount.toLamports("0.123456789"))
    }

    @Test
    fun moreThanNineDecimalsIsRefusedRatherThanRounded() {
        assertNull(SolanaAmount.toLamports("0.0000000001"))
        assertNull(SolanaAmount.toLamports("1.1234567891"))
    }

    @Test
    fun zeroNegativeAndNonNumericAreRefused() {
        assertNull(SolanaAmount.toLamports("0"))
        assertNull(SolanaAmount.toLamports("-1"))
        assertNull(SolanaAmount.toLamports(""))
        assertNull(SolanaAmount.toLamports("1.2.3"))
        assertNull(SolanaAmount.toLamports("all of it"))
    }

    @Test
    fun anAmountTooLargeForLamportsIsRefused() {
        assertNull(SolanaAmount.toLamports("100000000000"))
    }

    @Test
    fun formattingRoundTrips() {
        assertEquals("1.5", SolanaAmount.format(1_500_000_000L))
        assertEquals("0.000000001", SolanaAmount.format(1L))
    }
}
