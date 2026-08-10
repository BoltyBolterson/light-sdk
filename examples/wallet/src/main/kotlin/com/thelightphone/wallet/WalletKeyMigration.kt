package com.thelightphone.wallet

internal object WalletKeyMigration {

    fun run(database: WalletDatabase) {
        val legacy = WalletKeystore(WalletKeystore.LEGACY_KEY_ALIAS)
        if (!legacy.exists()) return

        val legacyCipher = WalletKeyCipher(legacy, ensureKey = false)
        val current = WalletKeyCipher()

        val seedDao = database.seedDao()
        seedDao.getAll().forEach { seed ->
            reencrypt(legacyCipher, current, seed.encryptedEntropy)?.let {
                seedDao.updateEncryptedEntropy(seed.id, it)
            }
        }

        val cardDao = database.cardDao()
        cardDao.getAll().forEach { card ->
            reencrypt(legacyCipher, current, card.encryptedPayload)?.let {
                cardDao.updateEncryptedPayload(card.id, it)
            }
        }

        legacy.delete()
    }

    private fun reencrypt(
        legacy: WalletKeyCipher,
        current: WalletKeyCipher,
        blob: ByteArray,
    ): ByteArray? {
        val plaintext = runCatching { legacy.decrypt(blob) }.getOrNull() ?: return null
        try {
            return current.encrypt(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }
}
