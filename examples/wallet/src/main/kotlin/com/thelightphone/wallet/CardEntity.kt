package com.thelightphone.wallet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
internal data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "barcode_format") val barcodeFormat: String,
    @ColumnInfo(name = "encrypted_payload") val encryptedPayload: ByteArray,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
