package com.thelightphone.wallet

import com.thelightphone.wallet.crypto.Bip39
import java.security.SecureRandom

private const val ENTROPY_BYTES = 16

class WalletAccountRepository internal constructor(
    database: WalletDatabase,
    private val cipher: WalletKeyCipher,
) {
    private val dao = database.seedDao()

    fun listWallets(): List<WalletSummary> =
        dao.getAll().map { WalletSummary(id = it.id, name = it.name) }

    fun createWallet(name: String? = null): Long {
        val fresh = ByteArray(ENTROPY_BYTES).also { SecureRandom().nextBytes(it) }
        try {
            return dao.insertWithDefaultName(
                name = name,
                encryptedEntropy = cipher.encrypt(fresh),
                createdAt = System.currentTimeMillis(),
            )
        } finally {
            fresh.fill(0)
        }
    }

    fun renameWallet(id: Long, newName: String) {
        dao.updateName(id, newName)
    }

    fun listAccounts(walletId: Long, chains: Collection<Chain> = Chain.entries): List<WalletAccount> {
        val seed = mnemonicSeed(walletId)
        try {
            return chains.map { chain ->
                val generated = ChainKeyPair.deriveFrom(seed, chain)
                generated.privateKey.fill(0)
                WalletAccount(chain = chain, address = generated.address)
            }
        } finally {
            seed.fill(0)
        }
    }

    fun sendSol(walletId: Long, rpcUrl: String, destination: String, lamports: Long): String {
        val seed = mnemonicSeed(walletId)
        val generated = try {
            ChainKeyPair.deriveFrom(seed, Chain.SOLANA)
        } finally {
            seed.fill(0)
        }
        try {
            return SolanaSender(rpcUrl).send(generated.privateKey, destination, lamports)
        } finally {
            generated.privateKey.fill(0)
        }
    }

    private fun mnemonicSeed(walletId: Long): ByteArray {
        val entropy = entropy(walletId)
        try {
            return Bip39.mnemonicToSeed(Bip39.entropyToMnemonic(entropy))
        } finally {
            entropy.fill(0)
        }
    }

    private fun entropy(walletId: Long): ByteArray {
        val wallet = dao.getById(walletId) ?: error("No wallet with id $walletId")
        return cipher.decrypt(wallet.encryptedEntropy)
    }
}
