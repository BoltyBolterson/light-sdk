package com.thelightphone.wallet

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thelightphone.sdk.SealedLightContext

@Database(entities = [WalletSeedEntity::class, CardEntity::class], version = 2, exportSchema = true)
abstract class WalletDatabase : RoomDatabase() {
    internal abstract fun seedDao(): WalletSeedDao
    internal abstract fun cardDao(): CardDao

    companion object {
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SupportSQLiteDatabase) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cards` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`barcode_format` TEXT NOT NULL, " +
                        "`encrypted_payload` BLOB NOT NULL, " +
                        "`created_at` INTEGER NOT NULL)",
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wallet_seed_v2` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`encrypted_entropy` BLOB NOT NULL, " +
                        "`created_at` INTEGER NOT NULL)",
                )
                connection.execSQL(
                    "INSERT INTO `wallet_seed_v2` (`id`, `name`, `encrypted_entropy`, `created_at`) " +
                        "SELECT `id`, 'Wallet ' || (`id` + 1), `encrypted_entropy`, 0 FROM `wallet_seed`",
                )
                connection.execSQL("DROP TABLE `wallet_seed`")
                connection.execSQL("ALTER TABLE `wallet_seed_v2` RENAME TO `wallet_seed`")
            }
        }

        internal val MIGRATIONS = arrayOf(MIGRATION_1_2)

        fun build(lightContext: SealedLightContext): WalletDatabase =
            Room.databaseBuilder(
                lightContext.applicationContext,
                WalletDatabase::class.java,
                WalletStore.DATABASE_NAME,
            )
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
