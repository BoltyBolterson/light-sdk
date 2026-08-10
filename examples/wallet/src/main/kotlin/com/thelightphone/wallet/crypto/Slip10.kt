package com.thelightphone.wallet.crypto

/** SLIP-0010 key derivation for ed25519 (solana). Every index is hardened, ed25519 can't do normal. */
internal object Slip10Ed25519 {
    data class ExtendedKey(val privateKey: ByteArray, val chainCode: ByteArray)

    fun masterKeyFromSeed(seed: ByteArray): ExtendedKey {
        val i = hmacSha512(key = "ed25519 seed".toByteArray(Charsets.US_ASCII), data = seed)
        return ExtendedKey(i.take(32).toByteArray(), i.takeLast(32).toByteArray())
    }

    fun deriveChild(parent: ExtendedKey, index: Long): ExtendedKey {
        val hardenedIndex = index + Bip32.HARDENED_OFFSET
        val indexBytes = ser32(hardenedIndex)
        val data = byteArrayOf(0x00) + parent.privateKey + indexBytes

        val i = hmacSha512(key = parent.chainCode, data = data)
        return ExtendedKey(i.take(32).toByteArray(), i.takeLast(32).toByteArray())
    }

    /** Full path, e.g. "m/44'/501'/0'/0'". */
    fun derivePath(seed: ByteArray, path: String): ExtendedKey {
        var key = masterKeyFromSeed(seed)
        for (segment in parsePath(path)) {
            key = deriveChild(key, segment - Bip32.HARDENED_OFFSET)
        }
        return key
    }
}
