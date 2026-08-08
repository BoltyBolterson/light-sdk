package com.thelightphone.wallet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_seed")
internal data class WalletSeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "encrypted_entropy") val encryptedEntropy: ByteArray,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
