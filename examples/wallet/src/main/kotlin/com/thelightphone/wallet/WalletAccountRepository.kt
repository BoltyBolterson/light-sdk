package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip39
import java.security.SecureRandom

private const val ENTROPY_BYTES = 16 // 128 bits -> 12-word mnemonic, see Bip39.kt

/**
 * Owns every on-device wallet: each wallet is its own BIP-39 mnemonic, persisted only as
 * AES-GCM-wrapped entropy (never the derived seed or any chain private key), mirroring
 * TotpAccountRepository's singleton + encrypt-on-write pattern from examples/authenticator.
 * Every chain account for a given wallet is derived fresh from that wallet's seed on demand -
 * see ChainKeyPair.
 */
class WalletAccountRepository private constructor(
    database: WalletDatabase,
    private val cipher: WalletKeyCipher,
) {
    private val dao = database.seedDao()

    /** All wallets on device, ordered by creation time. */
    fun listWallets(): List<WalletSummary> =
        dao.getAll().map { WalletSummary(id = it.id, name = it.name) }

    /** Generates a fresh mnemonic and persists it as a new wallet row. When [name] is omitted
     * (e.g. the implicit first-run wallet), defaults to "Wallet N" where N is a simple
     * incrementing count of wallets that exist at creation time - not the raw row id, since
     * ids can have gaps. */
    fun createWallet(name: String? = null): Long {
        val fresh = ByteArray(ENTROPY_BYTES).also { SecureRandom().nextBytes(it) }
        return dao.insertWithDefaultName(
            name = name,
            encryptedEntropy = cipher.encrypt(fresh),
            createdAt = System.currentTimeMillis(),
        )
    }

    fun renameWallet(id: Long, newName: String) {
        dao.updateName(id, newName)
    }

    fun listAccounts(walletId: Long): List<WalletAccount> {
        val seed = mnemonicSeed(walletId)
        return Chain.entries.map { chain ->
            val generated = ChainKeyPair.deriveFrom(seed, chain)
            WalletAccount(chain = chain, address = generated.address)
        }
    }

    /** Decrypted private key bytes for [chain] on wallet [walletId], needed only at signing
     * time. */
    fun decryptPrivateKey(walletId: Long, chain: Chain): ByteArray =
        ChainKeyPair.deriveFrom(mnemonicSeed(walletId), chain).privateKey

    /** The 12-word recovery phrase for wallet [walletId], for a future "reveal backup phrase"
     * screen. Not persisted anywhere except as wrapped entropy - regenerated from that on
     * every call. */
    fun getMnemonicWords(walletId: Long): List<String> = Bip39.entropyToMnemonic(entropy(walletId))

    private fun mnemonicSeed(walletId: Long): ByteArray =
        Bip39.mnemonicToSeed(Bip39.entropyToMnemonic(entropy(walletId)))

    private fun entropy(walletId: Long): ByteArray {
        val wallet = dao.getById(walletId) ?: error("No wallet with id $walletId")
        return cipher.decrypt(wallet.encryptedEntropy)
    }

    companion object {
        const val DATABASE_NAME = "wallet_accounts.db"

        @Volatile
        private var instance: WalletAccountRepository? = null

        fun getInstance(databaseProvider: () -> WalletDatabase): WalletAccountRepository {
            return instance ?: synchronized(this) {
                instance ?: WalletAccountRepository(
                    database = databaseProvider(),
                    cipher = WalletKeyCipher(),
                ).also { instance = it }
            }
        }
    }
}
