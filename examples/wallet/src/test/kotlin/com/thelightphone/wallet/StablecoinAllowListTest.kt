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
        // Solana base58 mint addresses are mixed-case, and two addresses differing only by case
        // are two different addresses. isAllowed() must do an exact, case-sensitive match - a
        // future ignoreCase/.lowercase()/.trim() "normalization" would silently widen the
        // allow-list and must fail this test.
        val lowercasedMint = SolanaStablecoins.USDC.mintAddress.lowercase()

        // Sanity check: lowercasing the real mint address must actually change it, otherwise
        // this test wouldn't be exercising case-sensitivity at all.
        assertTrue(lowercasedMint != SolanaStablecoins.USDC.mintAddress)

        assertFalse(SolanaStablecoins.isAllowed(lowercasedMint))
    }

    @Test
    fun tokenClaimingUsdcSymbolWithWrongMintIsNotAllowed() {
        // A scam token can freely self-report symbol "USDC" - the allow-list must reject it
        // anyway because the check is keyed strictly by mint address, never by ticker/name.
        val scamUsdc = AllowedStablecoin(
            mintAddress = "ScamMintAddressThatIsNotTheRealUSDC11111111",
            symbol = "USDC",
            decimals = 6,
        )

        assertFalse(SolanaStablecoins.isAllowed(scamUsdc.mintAddress))
    }
}
