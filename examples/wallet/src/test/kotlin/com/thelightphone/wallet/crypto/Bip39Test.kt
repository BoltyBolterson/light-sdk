package com.thelightphone.wallet.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Fixtures from the canonical trezor/python-mnemonic test vectors (passphrase "TREZOR"). */
class Bip39Test {
    @Test
    fun allZeroEntropy() {
        val entropy = ByteArray(16) // 00000000000000000000000000000000
        val mnemonic = Bip39.entropyToMnemonic(entropy)

        assertEquals(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            mnemonic.joinToString(" "),
        )
        assertEquals(
            "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04",
            toHex(Bip39.mnemonicToSeed(mnemonic, passphrase = "TREZOR")),
        )
        assertTrue(Bip39.isValidMnemonic(mnemonic))
    }

    @Test
    fun repeatingByteEntropy() {
        val entropy = ByteArray(16) { 0x7f.toByte() }
        val mnemonic = Bip39.entropyToMnemonic(entropy)

        assertEquals(
            "legal winner thank year wave sausage worth useful legal winner thank yellow",
            mnemonic.joinToString(" "),
        )
        assertEquals(
            "2e8905819b8723fe2c1d161860e5ee1830318dbf49a83bd451cfb8440c28bd6fa457fe1296106559a3c80937a1c1069be3a3a5bd381ee6260e8d9739fce1f607",
            toHex(Bip39.mnemonicToSeed(mnemonic, passphrase = "TREZOR")),
        )
    }

    @Test
    fun corruptedMnemonicFailsValidation() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon"
            .split(" ")
        assertTrue(!Bip39.isValidMnemonic(mnemonic))
    }

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}
