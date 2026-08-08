package com.thelightphone.wallet.crypto

import java.security.MessageDigest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

/**
 * BIP-39: https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki
 * Mnemonic generation/validation and the mnemonic -> seed PBKDF2 function. English wordlist
 * only (all ASCII, so no NFKD normalization is needed - the spec requires it in general, but
 * it's a no-op for this wordlist).
 */
internal object Bip39 {
    private const val ENTROPY_BITS = 128 // 12-word mnemonic; 128 bits is what every major
    // wallet defaults to (Phantom, MetaMask, Sparrow) - 256-bit/24-word is supported by the
    // spec but is not a meaningfully stronger security margin for this purpose and asks more
    // of the user to safely transcribe. Not hardcoded elsewhere; change here if that tradeoff
    // should go the other way.
    private const val PBKDF2_ITERATIONS = 2048
    private const val SEED_BITS = 512

    fun entropyToMnemonic(entropy: ByteArray): List<String> {
        require(entropy.size * 8 in setOf(128, 160, 192, 224, 256)) {
            "entropy must be 128/160/192/224/256 bits, got ${entropy.size * 8}"
        }
        val checksumLength = entropy.size * 8 / 32
        val checksumByte = MessageDigest.getInstance("SHA-256").digest(entropy)[0]

        // entropy bits followed by the first `checksumLength` bits of SHA-256(entropy)
        val bits = StringBuilder()
        for (byte in entropy) {
            bits.append(byteToBits(byte))
        }
        bits.append(byteToBits(checksumByte).take(checksumLength))

        return bits.chunked(11).map { chunk -> Bip39Wordlist.WORDS[chunk.toInt(2)] }
    }

    /** Recomputes the checksum to confirm [mnemonic] is well-formed (e.g. for a "restore from
     * seed phrase" import flow) - not used by the key-generation path itself. */
    fun isValidMnemonic(mnemonic: List<String>): Boolean {
        if (mnemonic.size !in setOf(12, 15, 18, 21, 24)) return false
        val indices = mnemonic.map { word -> Bip39Wordlist.WORDS.indexOf(word).also { if (it < 0) return false } }
        val bits = indices.joinToString("") { it.toString(2).padStart(11, '0') }

        val entropyBits = bits.length * 32 / 33
        val entropyBytes = bits.take(entropyBits).chunked(8).map { it.toInt(2).toByte() }.toByteArray()
        val expectedChecksumBits = bits.substring(entropyBits)
        val actualChecksum = byteToBits(MessageDigest.getInstance("SHA-256").digest(entropyBytes)[0])
            .take(expectedChecksumBits.length)
        return actualChecksum == expectedChecksumBits
    }

    /** PBKDF2-HMAC-SHA512(mnemonic, salt = "mnemonic" + passphrase, 2048 rounds, 64-byte output). */
    fun mnemonicToSeed(mnemonic: List<String>, passphrase: String = ""): ByteArray {
        val password = mnemonic.joinToString(" ").toByteArray(Charsets.UTF_8)
        val salt = ("mnemonic" + passphrase).toByteArray(Charsets.UTF_8)

        val generator = PKCS5S2ParametersGenerator(SHA512Digest())
        generator.init(password, salt, PBKDF2_ITERATIONS)
        return (generator.generateDerivedParameters(SEED_BITS) as KeyParameter).key
    }

    private fun byteToBits(byte: Byte): String =
        (byte.toInt() and 0xFF).toString(2).padStart(8, '0')
}
