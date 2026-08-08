package com.thelightphone.wallet

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thelightphone.sdk.SealedLightContext

@Database(entities = [WalletSeedEntity::class, CardEntity::class], version = 2, exportSchema = false)
abstract class WalletDatabase : RoomDatabase() {
    internal abstract fun seedDao(): WalletSeedDao
    internal abstract fun cardDao(): CardDao

    companion object {
        /**
         * Builds this database with a destructive-migration fallback, bypassing the shared
         * [com.thelightphone.sdk.buildDatabase] (which has no migration path at all and crashes
         * on any version bump - see LightDb.kt). This database went v1 -> v2 when CardEntity was
         * added with no migration written, so every existing install would otherwise crash on
         * open. This example app is still pre-launch, so destructive fallback (drop and recreate
         * on an unhandled version change) is an acceptable trade for now: lost local data, not a
         * crash. Deliberately scoped to just this database - com.thelightphone.sdk.LightDb.kt is
         * shared SDK infra also used by examples/authenticator and shouldn't change behavior for
         * every consumer to patch a wallet-specific gap.
         */
        fun build(lightContext: SealedLightContext): WalletDatabase =
            Room.databaseBuilder(
                lightContext.applicationContext,
                WalletDatabase::class.java,
                WalletAccountRepository.DATABASE_NAME,
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
