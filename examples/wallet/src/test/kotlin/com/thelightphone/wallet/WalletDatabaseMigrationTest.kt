package com.thelightphone.wallet

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WalletDatabaseMigrationTest {

    private fun exportedVersions(): List<Int> {
        val root = File("schemas")
        assertTrue(root.isDirectory, "Room schema export is off; every version bump becomes unreviewable")
        return root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".json") }
            .mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }
            .toSortedSet()
            .toList()
    }

    @Test
    fun everyVersionGapHasAMigration() {
        val versions = exportedVersions()
        assertTrue(versions.isNotEmpty(), "no exported schemas found")

        val covered = WalletDatabase.MIGRATIONS.map { it.startVersion to it.endVersion }.toSet()
        val required = versions.zipWithNext().toSet()
        val missing = required - covered

        assertTrue(
            missing.isEmpty(),
            "no migration for $missing — a version bump without one destroys every stored seed",
        )
    }

    @Test
    fun theNewestExportedSchemaMatchesTheDeclaredVersion() {
        assertEquals(
            exportedVersions().last(),
            WalletDatabase.MIGRATIONS.maxOfOrNull { it.endVersion } ?: exportedVersions().last(),
            "the schema on disk and the migration chain disagree about the current version",
        )
    }
}
