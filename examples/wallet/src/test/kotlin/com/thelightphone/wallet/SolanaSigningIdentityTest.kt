package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip39
import kotlin.test.Test
import kotlin.test.assertEquals
import org.sol4k.Keypair

class SolanaSigningIdentityTest {

    private val phrase =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    private fun solanaPrivateKey(): ByteArray {
        val seed = Bip39.mnemonicToSeed(phrase.split(" "))
        return ChainKeyPair.deriveFrom(seed, Chain.SOLANA).privateKey
    }

    @Test
    fun theSignerAndTheDisplayedAddressAreTheSameAccount() {
        val privateKey = solanaPrivateKey()
        val displayed = ChainKeyPair.addressFor(Chain.SOLANA, privateKey)
        val signing = Keypair.fromSecretKey(privateKey).publicKey.toBase58()
        assertEquals(
            displayed,
            signing,
            "sol4k would sign from an account the user was never shown",
        )
    }

    @Test
    fun theDerivedAddressMatchesTheKnownVectorForThisPhrase() {
        assertEquals(
            "HAgk14JpMQLgt6rVgv7cBQFJWFto5Dqxi472uT3DKpqk",
            ChainKeyPair.addressFor(Chain.SOLANA, solanaPrivateKey()),
        )
    }
}
