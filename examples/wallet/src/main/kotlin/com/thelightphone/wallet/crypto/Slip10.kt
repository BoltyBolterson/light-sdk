package com.thelightphone.wallet.crypto

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter

/**
 * SLIP-0010 hierarchical deterministic key derivation for ed25519 (Solana):
 * https://github.com/satoshilabs/slips/blob/master/slip-0010.md
 * ed25519 only supports *hardened* derivation (there's no public-key math to derive a normal
 * child from, unlike secp256k1's BIP-32) - every index here is implicitly hardened, matching
 * every mainstream Solana wallet's convention. Verified against the spec's ed25519 Test Vector 1
 * in Slip10Test.kt.
 */
internal object Slip10Ed25519 {
    data class ExtendedKey(val privateKey: ByteArray, val chainCode: ByteArray)

    fun masterKeyFromSeed(seed: ByteArray): ExtendedKey {
        val i = hmacSha512(key = "ed25519 seed".toByteArray(Charsets.US_ASCII), data = seed)
        return ExtendedKey(i.take(32).toByteArray(), i.takeLast(32).toByteArray())
    }

    /** [index] is un-hardened (e.g. 0, 1, 2...) - this always derives it as hardened, per spec. */
    fun deriveChild(parent: ExtendedKey, index: Long): ExtendedKey {
        val hardenedIndex = index + Bip32.HARDENED_OFFSET
        val indexBytes = ByteArray(4) { i -> ((hardenedIndex shr (24 - 8 * i)) and 0xFF).toByte() }
        val data = byteArrayOf(0x00) + parent.privateKey + indexBytes

        val i = hmacSha512(key = parent.chainCode, data = data)
        return ExtendedKey(i.take(32).toByteArray(), i.takeLast(32).toByteArray())
    }

    /** Derives along a path of plain (implicitly-hardened) indices, e.g. "m/44'/501'/0'/0'". */
    fun derivePath(seed: ByteArray, path: String): ExtendedKey {
        var key = masterKeyFromSeed(seed)
        for (segment in parsePath(path)) {
            key = deriveChild(key, segment - Bip32.HARDENED_OFFSET)
        }
        return key
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
