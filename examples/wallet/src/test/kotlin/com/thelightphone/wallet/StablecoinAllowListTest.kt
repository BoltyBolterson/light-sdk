package com.thelightphone.wallet

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StablecoinAllowListTest {
    @Test
    fun realUsdcMintIsAllowed() {
        assertTrue(SolanaStablecoins.isAllowed(SolanaStablecoins.USDC.mintAddress))
    }

    @Test
    fun realUsdtMintIsAllowed() {
        assertTrue(SolanaStablecoins.isAllowed(SolanaStablecoins.USDT.mintAddress))
    }

    @Test
    fun arbitraryFakeAddressIsNotAllowed() {
        assertFalse(SolanaStablecoins.isAllowed("11111111111111111111111111111111111111111"))
    }

    @Test
    fun lowercasedUsdcMintIsNotAllowed() {
        val lowercasedMint = SolanaStablecoins.USDC.mintAddress.lowercase()
        assertTrue(lowercasedMint != SolanaStablecoins.USDC.mintAddress)

        assertFalse(SolanaStablecoins.isAllowed(lowercasedMint))
    }
}
