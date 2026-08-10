package com.thelightphone.wallet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val SOLANA = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
private const val BITCOIN_BASE58 = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
private const val BITCOIN_P2SH = "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"
private const val BITCOIN_BECH32 = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
private const val BITCOIN_TAPROOT =
    "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0"
private const val ETHEREUM = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"

class AddressValidatorTest {

    @Test
    fun acceptsSolanaAddress() {
        assertEquals(SOLANA, AddressValidator.validate(SOLANA, Chain.SOLANA))
    }

    @Test
    fun acceptsBitcoinBase58CheckAddresses() {
        assertEquals(BITCOIN_BASE58, AddressValidator.validate(BITCOIN_BASE58, Chain.BITCOIN))
        assertEquals(BITCOIN_P2SH, AddressValidator.validate(BITCOIN_P2SH, Chain.BITCOIN))
    }

    @Test
    fun acceptsBitcoinBech32Addresses() {
        assertEquals(BITCOIN_BECH32, AddressValidator.validate(BITCOIN_BECH32, Chain.BITCOIN))
        assertEquals(BITCOIN_TAPROOT, AddressValidator.validate(BITCOIN_TAPROOT, Chain.BITCOIN))
        assertEquals(
            BITCOIN_BECH32.uppercase(),
            AddressValidator.validate(BITCOIN_BECH32.uppercase(), Chain.BITCOIN),
        )
    }

    @Test
    fun acceptsEthereumAddressInEitherSingleCase() {
        assertEquals(ETHEREUM, AddressValidator.validate(ETHEREUM, Chain.ETHEREUM))
        val lowercased = "0x" + ETHEREUM.drop(2).lowercase()
        val uppercased = "0x" + ETHEREUM.drop(2).uppercase()
        assertEquals(lowercased, AddressValidator.validate(lowercased, Chain.ETHEREUM))
        assertEquals(uppercased, AddressValidator.validate(uppercased, Chain.ETHEREUM))
    }

    @Test
    fun rejectsMixedCaseEthereumAddressFailingEip55() {
        val tampered = "0x5AAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
        assertNull(AddressValidator.validate(tampered, Chain.ETHEREUM))
    }

    @Test
    fun rejectsBitcoinAddressesWithBrokenChecksums() {
        assertNull(AddressValidator.validate("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb", Chain.BITCOIN))
        assertNull(
            AddressValidator.validate("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5", Chain.BITCOIN),
        )
    }

    @Test
    fun rejectsWrongChainAddresses() {
        assertNull(AddressValidator.validate(ETHEREUM, Chain.SOLANA))
        assertNull(AddressValidator.validate(SOLANA, Chain.ETHEREUM))
        assertNull(AddressValidator.validate(SOLANA, Chain.BITCOIN))
        assertNull(AddressValidator.validate(BITCOIN_BECH32, Chain.ETHEREUM))
        assertNull(AddressValidator.validate(BITCOIN_BECH32, Chain.SOLANA))
    }

    @Test
    fun stripsRightToLeftOverride() {
        val payload = SOLANA.take(10) + '\u202E' + SOLANA.drop(10)
        assertEquals(SOLANA, AddressValidator.validate(payload, Chain.SOLANA))
    }

    @Test
    fun stripsZeroWidthJoinerAndSpace() {
        val payload = SOLANA.take(4) + '\u200D' + SOLANA.drop(4) + '\u200B'
        assertEquals(SOLANA, AddressValidator.validate(payload, Chain.SOLANA))
    }

    @Test
    fun stripsSurroundingWhitespaceAndNewlines() {
        assertEquals(SOLANA, AddressValidator.validate("\n  $SOLANA \r\n", Chain.SOLANA))
    }

    @Test
    fun rejectsMultiLinePayload() {
        assertNull(AddressValidator.validate("$SOLANA\nhttps://drainer.example", Chain.SOLANA))
        assertNull(AddressValidator.validate("$BITCOIN_BASE58\n$BITCOIN_BASE58", Chain.BITCOIN))
    }

    @Test
    fun rejectsOverLengthPayloadBeforeSanitising() {
        val padded = SOLANA + "\u200B".repeat(200)
        assertNull(AddressValidator.validate(padded, Chain.SOLANA))
        assertNull(AddressValidator.validate("A".repeat(5000), Chain.SOLANA))
    }

    @Test
    fun rejectsEmptyAndGarbagePayloads() {
        Chain.entries.forEach {
            assertNull(AddressValidator.validate("", it))
            assertNull(AddressValidator.validate("https://drainer.example", it))
        }
    }

    @Test
    fun validateAnyMatchesOnlyOfferedChains() {
        assertEquals(SOLANA, AddressValidator.validateAny(SOLANA, Chain.entries))
        assertNull(AddressValidator.validateAny(SOLANA, listOf(Chain.BITCOIN, Chain.ETHEREUM)))
        assertNull(AddressValidator.validateAny(ETHEREUM, emptyList()))
    }
}
