package com.thelightphone.wallet

class CardRepository internal constructor(
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

    fun deleteCard(id: Long) {
        dao.delete(id)
    }

    fun getDecryptedPayload(id: Long): String {
        val card = dao.getById(id) ?: error("No card with id $id")
        return String(cipher.decrypt(card.encryptedPayload), Charsets.UTF_8)
    }
}
