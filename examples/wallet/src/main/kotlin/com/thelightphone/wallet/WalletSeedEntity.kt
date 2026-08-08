package com.thelightphone.wallet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per on-device wallet, holding that wallet's AES-GCM-wrapped BIP-39 entropy every
 * chain key is derived from. See WalletKeyCipher for the wrapping and ChainKeyPair for
 * derivation. [createdAt] is wall-clock time at insert, used only for default list ordering -
 * never for anything security-sensitive. */
@Entity(tableName = "wallet_seed")
internal data class WalletSeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "encrypted_entropy") val encryptedEntropy: ByteArray,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
