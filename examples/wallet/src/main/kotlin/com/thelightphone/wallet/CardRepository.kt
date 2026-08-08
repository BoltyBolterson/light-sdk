package com.thelightphone.wallet

/**
 * Owns stored gift card / membership / ticket barcodes: persists only the AES-GCM-wrapped
 * payload (never plaintext barcode data at rest), mirroring WalletAccountRepository's
 * singleton + encrypt-on-write pattern and reusing the same WalletKeyCipher.
 */
class CardRepository private constructor(
    database: WalletDatabase,
    private val cipher: WalletKeyCipher,
) {
    private val dao = database.cardDao()

    fun listCards(): List<CardSummary> =
        dao.getAll().map { CardSummary(id = it.id, name = it.name, barcodeFormat = it.barcodeFormat) }

    fun addCard(name: String, barcodeFormat: String, payload: String): Long {
        return dao.insert(
            CardEntity(
                name = name,
                barcodeFormat = barcodeFormat,
                encryptedPayload = cipher.encrypt(payload.toByteArray(Charsets.UTF_8)),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun getDecryptedPayload(id: Long): String {
        val card = dao.getById(id) ?: error("No card with id $id")
        return String(cipher.decrypt(card.encryptedPayload), Charsets.UTF_8)
    }

    fun deleteCard(id: Long) {
        dao.delete(id)
    }

    companion object {
        @Volatile
        private var instance: CardRepository? = null

        fun getInstance(databaseProvider: () -> WalletDatabase): CardRepository {
            return instance ?: synchronized(this) {
                instance ?: CardRepository(
                    database = databaseProvider(),
                    cipher = WalletKeyCipher(),
                ).also { instance = it }
            }
        }
    }
}
