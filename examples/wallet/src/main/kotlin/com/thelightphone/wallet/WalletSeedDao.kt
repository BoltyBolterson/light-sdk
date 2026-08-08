package com.thelightphone.wallet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
internal interface WalletSeedDao {
    @Insert
    fun insert(wallet: WalletSeedEntity): Long

    @Query("SELECT * FROM wallet_seed ORDER BY created_at ASC")
    fun getAll(): List<WalletSeedEntity>

    @Query("SELECT * FROM wallet_seed WHERE id = :id")
    fun getById(id: Long): WalletSeedEntity?

    @Query("UPDATE wallet_seed SET name = :name WHERE id = :id")
    fun updateName(id: Long, name: String)

    @Transaction
    fun insertWithDefaultName(name: String?, encryptedEntropy: ByteArray, createdAt: Long): Long {
        val resolvedName = name ?: "Wallet ${getAll().size + 1}"
        return insert(
            WalletSeedEntity(
                name = resolvedName,
                encryptedEntropy = encryptedEntropy,
                createdAt = createdAt,
            ),
        )
    }
}
