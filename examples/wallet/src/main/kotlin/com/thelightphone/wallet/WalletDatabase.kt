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
        fun build(lightContext: SealedLightContext): WalletDatabase =
            Room.databaseBuilder(
                lightContext.applicationContext,
                WalletDatabase::class.java,
                WalletStore.DATABASE_NAME,
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
