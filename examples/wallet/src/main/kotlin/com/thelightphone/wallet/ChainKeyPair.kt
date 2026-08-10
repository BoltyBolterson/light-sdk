package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip32
import com.thelightphone.wallet.crypto.Slip10Ed25519
import java.security.MessageDigest
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.jcajce.provider.digest.Keccak

internal object ChainKeyPair {
    data class Generated(val privateKey: ByteArray, val address: String)

    private const val BITCOIN_PATH = "m/44'/0'/0'/0/0"
    private const val ETHEREUM_PATH = "m/44'/60'/0'/0/0"
    private const val SOLANA_PATH = "m/44'/501'/0'/0'" // what phantom, solflare and ledger use

    fun deriveFrom(seed: ByteArray, chain: Chain): Generated {
        val privateKey = when (chain) {
            Chain.SOLANA -> Slip10Ed25519.derivePath(seed, SOLANA_PATH).run { chainCode.fill(0); privateKey }
            Chain.BITCOIN -> Bip32.derivePath(seed, BITCOIN_PATH).run { chainCode.fill(0); privateKey }
            Chain.ETHEREUM -> Bip32.derivePath(seed, ETHEREUM_PATH).run { chainCode.fill(0); privateKey }
        }
        return Generated(privateKey, addressFor(chain, privateKey))
    }

    fun addressFor(chain: Chain, privateKey: ByteArray): String = when (chain) {
        Chain.SOLANA -> {
            val priv = Ed25519PrivateKeyParameters(privateKey, 0)
            solanaAddress(priv.generatePublicKey().encoded)
        }
        Chain.BITCOIN -> bitcoinAddress(Bip32.publicKeyFor(privateKey, compressed = true))
        Chain.ETHEREUM -> ethereumAddress(Bip32.publicKeyFor(privateKey, compressed = false))
    }

    private fun solanaAddress(publicKey: ByteArray): String = Base58.encode(publicKey)

    private fun bitcoinAddress(compressedPublicKey: ByteArray): String {
        val sha256 = MessageDigest.getInstance("SHA-256").digest(compressedPublicKey)
        val ripemd160 = RIPEMD160Digest().run {
            update(sha256, 0, sha256.size)
            ByteArray(digestSize).also { doFinal(it, 0) }
        }
        return Base58.encodeCheck(byteArrayOf(0x00) + ripemd160)
    }

    private fun ethereumAddress(uncompressedPublicKey: ByteArray): String {
        val digest = Keccak.Digest256().digest(uncompressedPublicKey.copyOfRange(1, uncompressedPublicKey.size))
        val addressBytes = digest.copyOfRange(digest.size - 20, digest.size)
        return "0x" + addressBytes.joinToString("") { "%02x".format(it) }
    }
}
