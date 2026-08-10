package com.thelightphone.wallet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

private const val RENT_EXEMPT_MINIMUM = 890_880L

class SolanaSendCostTest {

    @Test
    fun aBalanceCoveringAmountFeeAndRentIsAffordable() {
        assertIs<SendAffordability.Affordable>(
            affordSend(
                balance = 2_000_000_000L,
                amount = 1_000_000_000L,
                fee = LAMPORTS_PER_SIGNATURE,
                rentExemptMinimum = RENT_EXEMPT_MINIMUM,
            ),
        )
    }

    @Test
    fun balanceExactlyEqualToAmountPlusFeeIsAffordable() {
        assertIs<SendAffordability.Affordable>(
            affordSend(
                balance = 1_000_000_000L + LAMPORTS_PER_SIGNATURE,
                amount = 1_000_000_000L,
                fee = LAMPORTS_PER_SIGNATURE,
                rentExemptMinimum = RENT_EXEMPT_MINIMUM,
            ),
        )
    }

    @Test
    fun oneLamportOverTheBalanceIsShortByOneLamport() {
        val outcome = affordSend(
            balance = 1_000_000_000L + LAMPORTS_PER_SIGNATURE,
            amount = 1_000_000_000L + 1L,
            fee = LAMPORTS_PER_SIGNATURE,
            rentExemptMinimum = RENT_EXEMPT_MINIMUM,
        )
        assertEquals(1L, assertIs<SendAffordability.ShortBy>(outcome).lamports)
    }

    @Test
    fun theFeeAloneCanMakeAFullBalanceUnaffordable() {
        val outcome = affordSend(
            balance = 1_000_000_000L,
            amount = 1_000_000_000L,
            fee = LAMPORTS_PER_SIGNATURE,
            rentExemptMinimum = RENT_EXEMPT_MINIMUM,
        )
        assertEquals(LAMPORTS_PER_SIGNATURE, assertIs<SendAffordability.ShortBy>(outcome).lamports)
    }

    @Test
    fun aBalanceBelowTheFeeIsShortByTheWholeDifference() {
        val outcome = affordSend(
            balance = 1_000L,
            amount = 1L,
            fee = LAMPORTS_PER_SIGNATURE,
            rentExemptMinimum = RENT_EXEMPT_MINIMUM,
        )
        assertEquals(4_001L, assertIs<SendAffordability.ShortBy>(outcome).lamports)
    }

    @Test
    fun aRemainderUnderTheRentExemptMinimumIsRefusedWithBothCeilings() {
        val balance = 2_000_000_000L
        val outcome = affordSend(
            balance = balance,
            amount = balance - LAMPORTS_PER_SIGNATURE - 1L,
            fee = LAMPORTS_PER_SIGNATURE,
            rentExemptMinimum = RENT_EXEMPT_MINIMUM,
        )
        val stranded = assertIs<SendAffordability.StrandsRent>(outcome)
        assertEquals(balance - LAMPORTS_PER_SIGNATURE - RENT_EXEMPT_MINIMUM, stranded.maxKeepingAccount)
        assertEquals(balance - LAMPORTS_PER_SIGNATURE, stranded.maxDraining)
    }

    @Test
    fun drainingTheAccountToExactlyZeroIsAffordable() {
        val balance = 2_000_000_000L
        assertIs<SendAffordability.Affordable>(
            affordSend(
                balance = balance,
                amount = balance - LAMPORTS_PER_SIGNATURE,
                fee = LAMPORTS_PER_SIGNATURE,
                rentExemptMinimum = RENT_EXEMPT_MINIMUM,
            ),
        )
    }

    @Test
    fun aRemainderExactlyAtTheRentExemptMinimumIsAffordable() {
        val balance = 2_000_000_000L
        assertIs<SendAffordability.Affordable>(
            affordSend(
                balance = balance,
                amount = balance - LAMPORTS_PER_SIGNATURE - RENT_EXEMPT_MINIMUM,
                fee = LAMPORTS_PER_SIGNATURE,
                rentExemptMinimum = RENT_EXEMPT_MINIMUM,
            ),
        )
    }

    @Test
    fun anAmountNearTheLongCeilingReportsAShortfallRatherThanWrapping() {
        val outcome = affordSend(
            balance = 0L,
            amount = Long.MAX_VALUE,
            fee = LAMPORTS_PER_SIGNATURE,
            rentExemptMinimum = RENT_EXEMPT_MINIMUM,
        )
        assertEquals(Long.MAX_VALUE, assertIs<SendAffordability.ShortBy>(outcome).lamports)
    }

    @Test
    fun anAmountOfZeroOrLessIsRejectedOutright() {
        assertFailsWith<IllegalArgumentException> {
            affordSend(1_000L, 0L, LAMPORTS_PER_SIGNATURE, RENT_EXEMPT_MINIMUM)
        }
    }
}
