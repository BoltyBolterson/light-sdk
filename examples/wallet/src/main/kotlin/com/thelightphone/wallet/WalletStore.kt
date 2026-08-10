package com.thelightphone.wallet

import com.thelightphone.sdk.SealedLightContext

object WalletStore {
    const val DATABASE_NAME = "wallet_accounts.db"

    @Volatile
    private var database: WalletDatabase? = null

    @Volatile
    private var accountRepository: WalletAccountRepository? = null

    @Volatile
    private var cardRepository: CardRepository? = null

    private var cipher: WalletKeyCipher? = null

    fun accounts(lightContext: SealedLightContext): WalletAccountRepository =
        accountRepository ?: synchronized(this) {
            accountRepository ?: WalletAccountRepository(database(lightContext), cipher())
                .also { accountRepository = it }
        }

    fun cards(lightContext: SealedLightContext): CardRepository =
        cardRepository ?: synchronized(this) {
            cardRepository ?: CardRepository(database(lightContext), cipher())
                .also { cardRepository = it }
        }

    private fun database(lightContext: SealedLightContext): WalletDatabase =
        database ?: WalletDatabase.build(lightContext)
            .also { WalletKeyMigration.run(it) }
            .also { database = it }

    private fun cipher(): WalletKeyCipher = cipher ?: WalletKeyCipher().also { cipher = it }
}
