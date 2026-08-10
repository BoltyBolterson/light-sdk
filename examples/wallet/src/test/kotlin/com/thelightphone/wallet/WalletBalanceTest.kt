package com.thelightphone.wallet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WalletBalanceTest {

    @Test
    fun anUnreadBalanceIsNotZero() {
        val unread = WalletBalance.Unread.display(Chain.SOLANA)
        assertNotEquals("0 SOL", unread, "a balance we never read must not be shown as an empty wallet")
        assertEquals("—", unread)
    }

    @Test
    fun aFailedReadIsNotZero() {
        val unavailable = WalletBalance.Unavailable.display(Chain.SOLANA)
        assertNotEquals("0 SOL", unavailable)
        assertEquals("unavailable", unavailable)
    }

    @Test
    fun zeroIsRenderedAsZero() {
        assertEquals("0 SOL", WalletBalance.Lamports(0L).display(Chain.SOLANA))
    }

    @Test
    fun aReadBalanceIsFormattedInWholeSol() {
        assertEquals("4.2 SOL", WalletBalance.Lamports(4_200_000_000L).display(Chain.SOLANA))
        assertEquals("0.000000001 SOL", WalletBalance.Lamports(1L).display(Chain.SOLANA))
    }

    @Test
    fun chainsWithoutABalanceSourceRenderNothing() {
        assertNull(WalletBalance.Unsupported.display(Chain.BITCOIN))
        assertNull(WalletBalance.Unsupported.display(Chain.ETHEREUM))
    }

    @Test
    fun onlySolanaStartsOutAsUnread() {
        assertEquals(WalletBalance.Unread, Chain.SOLANA.unreadBalance())
        assertEquals(WalletBalance.Unsupported, Chain.BITCOIN.unreadBalance())
        assertEquals(WalletBalance.Unsupported, Chain.ETHEREUM.unreadBalance())
    }

    @Test
    fun onlyAnActualReadCountsAsRead() {
        assertTrue(WalletBalance.Lamports(0L).isRead())
        assertFalse(WalletBalance.Unread.isRead())
        assertFalse(WalletBalance.Unavailable.isRead())
        assertFalse(WalletBalance.Unsupported.isRead())
    }
}
