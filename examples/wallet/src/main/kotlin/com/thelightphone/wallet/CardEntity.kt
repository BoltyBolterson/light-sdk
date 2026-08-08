package com.thelightphone.wallet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per stored gift card / membership / ticket barcode. [barcodeFormat] mirrors ML
 * Kit's Barcode.FORMAT_* naming (e.g. "QR_CODE", "CODE_128", "EAN_13") stored as plain text -
 * this module takes no dependency on ML Kit itself; a later pass maps the string to a real
 * scanner/renderer. [encryptedPayload] is the barcode data/text, AES-GCM-wrapped via
 * WalletKeyCipher exactly like wallet seed entropy. */
@Entity(tableName = "cards")
internal data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "barcode_format") val barcodeFormat: String,
    @ColumnInfo(name = "encrypted_payload") val encryptedPayload: ByteArray,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
