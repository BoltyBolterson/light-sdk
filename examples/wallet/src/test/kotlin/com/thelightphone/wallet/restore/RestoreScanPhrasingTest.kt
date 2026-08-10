package com.thelightphone.wallet.restore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestoreScanPhrasingTest {

    @Test
    fun nothingFoundIsOnlySaidWhenEveryAddressAnswered() {
        val clean = summarize(List(4) { AddressStatus.Empty }, addressesExpected = 4)
        assertEquals("Nothing found", scanHeadline(clean))
        assertNull(scanCaveat(clean))
    }

    @Test
    fun anUnreadableAddressIsNeverPhrasedAsNothingFound() {
        val degraded = summarize(List(3) { AddressStatus.Empty } + AddressStatus.Unknown, addressesExpected = 4)
        assertTrue("Nothing found" !in scanHeadline(degraded))
        assertNotNull(scanCaveat(degraded)).let {
            assertTrue("1 of 4" in it)
            assertTrue("not proof" in it)
        }
    }

    @Test
    fun anAbandonedScanIsNeverPhrasedAsNothingFound() {
        val partial = summarize(List(2) { AddressStatus.Empty }, addressesExpected = 4)
        assertTrue("Nothing found" !in scanHeadline(partial))
        assertTrue("2 of 4" in assertNotNull(scanCaveat(partial)))
    }

    @Test
    fun fundsAreReportedEvenWhenTheScanIsInconclusive() {
        val mixed = summarize(
            listOf(AddressStatus.Funded(1L, false), AddressStatus.Unknown),
            addressesExpected = 2,
        )
        assertEquals("Funds on 1 of 2", scanHeadline(mixed))
        assertNotNull(scanCaveat(mixed), "a funded hit does not excuse an unread address")
    }

    @Test
    fun unknownAddressesReadAsUncheckedNotEmpty() {
        assertEquals("couldn't check", statusLabel(AddressStatus.Unknown))
        assertEquals("empty", statusLabel(AddressStatus.Empty))
    }

    @Test
    fun fundedAddressesShowWhatTheyHold() {
        assertEquals("4.2 SOL", statusLabel(AddressStatus.Funded(4_200_000_000L, false)))
        assertEquals("tokens", statusLabel(AddressStatus.Funded(0L, true)))
        assertEquals("1 SOL + tokens", statusLabel(AddressStatus.Funded(1_000_000_000L, true)))
    }
}
