package com.thelightphone.wallet.crypto

import java.math.BigInteger
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter

/** BIP-32 key derivation over secp256k1 (bitcoin, ethereum). Private parent to private child only. */
internal object Bip32 {
    const val HARDENED_OFFSET: Long = 0x80000000L

    data class ExtendedKey(val privateKey: ByteArray, val chainCode: ByteArray)

    fun masterKeyFromSeed(seed: ByteArray): ExtendedKey {
        val i = hmacSha512(key = "Bitcoin seed".toByteArray(Charsets.US_ASCII), data = seed)
        val (il, ir) = i.take(32).toByteArray() to i.takeLast(32).toByteArray()
        require(isValidPrivateKey(il)) { "invalid master key from this seed (astronomically unlikely - try re-generating)" }
        return ExtendedKey(il, ir)
    }

    fun deriveChild(parent: ExtendedKey, index: Long): ExtendedKey {
        val indexBytes = ByteArray(4) { i -> ((index shr (24 - 8 * i)) and 0xFF).toByte() }
        val data = if (index >= HARDENED_OFFSET) {
            byteArrayOf(0x00) + parent.privateKey + indexBytes
        } else {
            publicKeyFor(parent.privateKey) + indexBytes
        }

        val i = hmacSha512(key = parent.chainCode, data = data)
        val il = i.take(32).toByteArray()
        val ir = i.takeLast(32).toByteArray()

        require(isValidPrivateKey(il)) { "invalid child key at index $index (astronomically unlikely - caller should retry index + 1)" }

        val childKey = (BigInteger(1, il) + BigInteger(1, parent.privateKey)).mod(Secp256k1.n)
        return ExtendedKey(childKey.toFixedBytes(32), ir)
    }

    /** Full path, e.g. "m/44'/60'/0'/0/0". */
    fun derivePath(seed: ByteArray, path: String): ExtendedKey {
        var key = masterKeyFromSeed(seed)
        for (segment in parsePath(path)) {
            key = deriveChild(key, segment)
        }
        return key
    }

    fun publicKeyFor(privateKey: ByteArray, compressed: Boolean = true): ByteArray =
        Secp256k1.domain.g.multiply(BigInteger(1, privateKey)).normalize().getEncoded(compressed)

    private fun isValidPrivateKey(key: ByteArray): Boolean {
        val value = BigInteger(1, key)
        return value != BigInteger.ZERO && value < Secp256k1.n
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val hmac = HMac(SHA512Digest())
        hmac.init(KeyParameter(key))
        hmac.update(data, 0, data.size)
        val out = ByteArray(hmac.macSize)
        hmac.doFinal(out, 0)
        return out
    }
}

internal fun parsePath(path: String): List<Long> {
    val segments = path.removePrefix("m").removePrefix("/").split("/").filter { it.isNotBlank() }
    return segments.map { segment ->
        if (segment.endsWith("'") || segment.endsWith("H")) {
            segment.dropLast(1).toLong() + Bip32.HARDENED_OFFSET
        } else {
            segment.toLong()
        }
    }
}

internal fun BigInteger.toFixedBytes(length: Int): ByteArray {
    val raw = toByteArray()
    return when {
        raw.size == length -> raw
        raw.size == length + 1 && raw[0] == 0.toByte() -> raw.copyOfRange(1, raw.size)
        raw.size < length -> ByteArray(length - raw.size) + raw
        else -> error("unexpected key length ${raw.size}, wanted $length")
    }
}
