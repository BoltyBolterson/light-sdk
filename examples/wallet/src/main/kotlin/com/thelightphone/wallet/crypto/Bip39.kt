package com.thelightphone.wallet.crypto

import java.security.MessageDigest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

/** BIP-39 mnemonic generation, validation and seed derivation. English wordlist only. */
internal object Bip39 {
    private const val ENTROPY_BITS = 128 // 12 words, same as every other wallet
    private const val PBKDF2_ITERATIONS = 2048
    private const val SEED_BITS = 512

    fun entropyToMnemonic(entropy: ByteArray): List<String> {
        require(entropy.size * 8 in setOf(128, 160, 192, 224, 256)) {
            "entropy must be 128/160/192/224/256 bits, got ${entropy.size * 8}"
        }
        val checksumLength = entropy.size * 8 / 32
        val digest = MessageDigest.getInstance("SHA-256").digest(entropy)
        try {
            val bits = StringBuilder()
            for (byte in entropy) {
                bits.append(byteToBits(byte))
            }
            bits.append(byteToBits(digest[0]).take(checksumLength))

            return bits.chunked(11).map { chunk -> Bip39Wordlist.WORDS[chunk.toInt(2)] }
        } finally {
            digest.fill(0)
        }
    }

    fun isValidMnemonic(mnemonic: List<String>): Boolean {
        if (mnemonic.size !in setOf(12, 15, 18, 21, 24)) return false
        val indices = mnemonic.map { word -> Bip39Wordlist.WORDS.indexOf(word).also { if (it < 0) return false } }
        val bits = indices.joinToString("") { it.toString(2).padStart(11, '0') }

        val entropyBits = bits.length * 32 / 33
        val entropyBytes = bits.take(entropyBits).chunked(8).map { it.toInt(2).toByte() }.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(entropyBytes)
        try {
            val expectedChecksumBits = bits.substring(entropyBits)
            val actualChecksum = byteToBits(digest[0]).take(expectedChecksumBits.length)
            return actualChecksum == expectedChecksumBits
        } finally {
            entropyBytes.fill(0)
            digest.fill(0)
        }
    }

    fun mnemonicToSeed(mnemonic: List<String>, passphrase: String = ""): ByteArray {
        val password = mnemonic.joinToString(" ").toByteArray(Charsets.UTF_8)
        val salt = ("mnemonic" + passphrase).toByteArray(Charsets.UTF_8)
        try {
            val generator = PKCS5S2ParametersGenerator(SHA512Digest())
            generator.init(password, salt, PBKDF2_ITERATIONS)
            return (generator.generateDerivedParameters(SEED_BITS) as KeyParameter).key
        } finally {
            password.fill(0)
            salt.fill(0)
        }
    }

    private fun byteToBits(byte: Byte): String =
        (byte.toInt() and 0xFF).toString(2).padStart(8, '0')
}
