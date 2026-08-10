package com.thelightphone.wallet.restore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestoreScanTest {

    @Test
    fun solBalanceMakesAnAddressFunded() {
        val status = classifyAddress(lamports = 4_200_000_000L, hasTokens = false)
        val funded = assertIs<AddressStatus.Funded>(status)
        assertEquals(4_200_000_000L, funded.lamports)
    }

    @Test
    fun tokensAloneMakeAnAddressFunded() {
        assertIs<AddressStatus.Funded>(classifyAddress(lamports = 0L, hasTokens = true))
    }

    @Test
    fun zeroSolAndNoTokensIsEmpty() {
        assertIs<AddressStatus.Empty>(classifyAddress(lamports = 0L, hasTokens = false))
    }

    @Test
    fun unreadableBalanceIsUnknownEvenWhenTokensReadCleanly() {
        assertIs<AddressStatus.Unknown>(classifyAddress(lamports = null, hasTokens = false))
    }

    @Test
    fun unreadableTokensOnAZeroSolAddressIsUnknownNotEmpty() {
        assertIs<AddressStatus.Unknown>(classifyAddress(lamports = 0L, hasTokens = null))
    }

    @Test
    fun unreadableTokensDoNotDowngradeAFundedSolAddress() {
        assertIs<AddressStatus.Funded>(classifyAddress(lamports = 1L, hasTokens = null))
    }

    @Test
    fun nothingFoundIsOnlyConclusiveWhenEveryReadSucceeded() {
        val clean = summarize(List(4) { AddressStatus.Empty }, addressesExpected = 4)
        assertTrue(clean.isConclusive, "four clean empties means we can say 'nothing here'")
        assertEquals(0, clean.funded)

        val degraded = summarize(List(3) { AddressStatus.Empty } + AddressStatus.Unknown, addressesExpected = 4)
        assertFalse(degraded.isConclusive, "one unreadable address makes 'nothing here' a guess")
        assertEquals(1, degraded.unreadable)
    }

    @Test
    fun summaryCountsEachStatusExactlyOnce() {
        val statuses = listOf(
            AddressStatus.Funded(1L, false),
            AddressStatus.Funded(0L, true),
            AddressStatus.Empty,
            AddressStatus.Unknown,
        )
        val summary = summarize(statuses, addressesExpected = 4)
        assertEquals(2, summary.funded)
        assertEquals(1, summary.empty)
        assertEquals(1, summary.unreadable)
        assertEquals(4, summary.resolved, "progress must account for every probe, including the failures")
    }

    @Test
    fun onlyZeroBalanceAddressesNeedTheTokenFollowUp() {
        assertTrue(needsTokenRead(0L), "a zero balance is the only case tokens can still change")
        assertFalse(needsTokenRead(5L), "an address holding SOL is already funded")
    }

    @Test
    fun unreadableAddressesAreNotProbedFurther() {
        assertFalse(needsTokenRead(null))
    }

    @Test
    fun anIncompleteScanIsNeverConclusive() {
        val partial = summarize(List(2) { AddressStatus.Empty }, addressesExpected = 4)
        assertFalse(partial.isConclusive, "two of four answered is not 'nothing here'")
        assertFalse(summarize(emptyList(), addressesExpected = 4).isConclusive)
    }
}
