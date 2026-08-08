package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip32
import com.thelightphone.wallet.crypto.Slip10Ed25519
import java.security.MessageDigest
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.jcajce.provider.digest.Keccak

/**
 * Derives per-chain keys + addresses from one BIP-39 seed, via BIP-32 (secp256k1: Bitcoin,
 * Ethereum) / SLIP-0010 (ed25519: Solana) - see crypto/Bip32.kt, crypto/Slip10.kt. Coin types
 * are SLIP-44's registry (https://github.com/satoshilabs/slips/blob/master/slip-0044.md):
 * 0 = Bitcoin, 60 = Ethereum, 501 = Solana. Uses BouncyCastle's lightweight (non-JCE-provider)
 * API throughout - no java.security.Security registration, no native/NDK component.
 *
 * Signing (transaction construction and broadcast) is not implemented yet - this only covers
 * key derivation and the address computation needed to show a "Receive" address for each chain.
 */
internal object ChainKeyPair {
    data class Generated(val privateKey: ByteArray, val address: String)

    private const val BITCOIN_PATH = "m/44'/0'/0'/0/0"
    private const val ETHEREUM_PATH = "m/44'/60'/0'/0/0"
    private const val SOLANA_PATH = "m/44'/501'/0'/0'" // Phantom/Solflare/Ledger convention

    fun deriveFrom(seed: ByteArray, chain: Chain): Generated {
        val privateKey = when (chain) {
            Chain.SOLANA -> Slip10Ed25519.derivePath(seed, SOLANA_PATH).privateKey
            Chain.BITCOIN -> Bip32.derivePath(seed, BITCOIN_PATH).privateKey
            Chain.ETHEREUM -> Bip32.derivePath(seed, ETHEREUM_PATH).privateKey
        }
        return Generated(privateKey, addressFor(chain, privateKey))
    }

    /** Re-derives the public address from a raw private key (no need to re-walk the HD path). */
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
        // version 0x00 = mainnet P2PKH
        return Base58.encodeCheck(byteArrayOf(0x00) + ripemd160)
    }

    private fun ethereumAddress(uncompressedPublicKey: ByteArray): String {
        // Drop the leading 0x04 uncompressed-point prefix before hashing.
        val digest = Keccak.Digest256().digest(uncompressedPublicKey.copyOfRange(1, uncompressedPublicKey.size))
        val addressBytes = digest.copyOfRange(digest.size - 20, digest.size)
        return "0x" + addressBytes.joinToString("") { "%02x".format(it) }
    }
}
