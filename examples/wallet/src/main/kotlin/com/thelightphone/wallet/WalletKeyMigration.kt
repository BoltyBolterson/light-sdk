package com.thelightphone.wallet

internal object WalletKeyMigration {

    fun run(database: WalletDatabase) {
        val legacy = WalletKeystore(WalletKeystore.LEGACY_KEY_ALIAS)
        if (!legacy.exists()) return

        val legacyCipher = WalletKeyCipher(legacy, ensureKey = false, legacy = null)
        val current = WalletKeyCipher()
        var complete = true

        val seedDao = database.seedDao()
        seedDao.getAll().forEach { seed ->
            val migrated = reencrypt(legacyCipher, current, seed.encryptedEntropy)
            if (migrated == null) complete = false else seedDao.updateEncryptedEntropy(seed.id, migrated)
        }

        val cardDao = database.cardDao()
        cardDao.getAll().forEach { card ->
            val migrated = reencrypt(legacyCipher, current, card.encryptedPayload)
            if (migrated == null) complete = false else cardDao.updateEncryptedPayload(card.id, migrated)
        }

        if (complete) legacy.delete()
    }

    private fun reencrypt(
        legacy: WalletKeyCipher,
        current: WalletKeyCipher,
        blob: ByteArray,
    ): ByteArray? {
        val plaintext = runCatching { legacy.decrypt(blob) }.getOrNull() ?: return null
        try {
            return runCatching { current.encrypt(plaintext) }.getOrNull()
        } finally {
            plaintext.fill(0)
        }
    }
}
