package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip39
import java.security.SecureRandom

private const val ENTROPY_BYTES = 16

class WalletAccountRepository private constructor(
    database: WalletDatabase,
    private val cipher: WalletKeyCipher,
) {
    private val dao = database.seedDao()

    fun listWallets(): List<WalletSummary> =
        dao.getAll().map { WalletSummary(id = it.id, name = it.name) }

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

    fun decryptPrivateKey(walletId: Long, chain: Chain): ByteArray =
        ChainKeyPair.deriveFrom(mnemonicSeed(walletId), chain).privateKey

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
