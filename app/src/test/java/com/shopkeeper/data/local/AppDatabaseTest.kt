package com.shopkeeper.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.shopkeeper.data.model.Product
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated migration tests and entity mapping validation for AppDatabase.
 */
class AppDatabaseTest {

    @Test
    fun `MIGRATION_3_4 migrates schema from version 3 to 4 with expected DDL statements`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlStatements = mutableListOf<String>()
        every { db.execSQL(capture(sqlStatements)) } just Runs

        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)

        AppDatabase.MIGRATION_3_4.migrate(db)

        assertEquals("Should execute exactly 4 DDL statements", 4, sqlStatements.size)
        assertTrue(
            "Must execute ALTER TABLE to add unit column with default 'pcs'",
            sqlStatements.contains("ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT 'pcs'")
        )
        assertTrue(
            "Must execute ALTER TABLE to add lowStockThreshold column with default 0",
            sqlStatements.contains("ALTER TABLE products ADD COLUMN lowStockThreshold INTEGER NOT NULL DEFAULT 0")
        )
        assertTrue(
            "Must execute ALTER TABLE to add isDeleted column with default 0",
            sqlStatements.contains("ALTER TABLE products ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        )
        assertTrue(
            "Must execute ALTER TABLE to add deletedAt column with default NULL",
            sqlStatements.contains("ALTER TABLE products ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        )
    }

    @Test
    fun `MIGRATION_3_4 preserves existing row data non-destructively without dropping table`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlStatements = mutableListOf<String>()
        every { db.execSQL(capture(sqlStatements)) } just Runs

        AppDatabase.MIGRATION_3_4.migrate(db)

        // Verifies zero destructive drops occurred - existing user data is completely preserved
        assertFalse("Must never drop table during v3 to v4 migration", sqlStatements.any { it.contains("DROP TABLE") })
        assertEquals(4, sqlStatements.size)
        assertTrue("All statements must be non-destructive column additions", sqlStatements.all { it.startsWith("ALTER TABLE products ADD COLUMN") })
    }

    @Test
    fun `ProductEntity defaults and domain mapping preserve new schema attributes`() {
        val entity = ProductEntity(
            id = "PROD_1",
            code = "CODE_1",
            name = "Test Product",
            category = "General",
            purchasePriceMinor = 1000L,
            salesPriceMinor = 1500L,
            quantity = 10,
            unit = "kg",
            lowStockThreshold = 5,
            isDeleted = false,
            deletedAt = null,
            updatedAt = "123456789"
        )

        val domain = entity.toDomain()
        assertEquals("kg", domain.unit)
        assertEquals(5, domain.lowStockThreshold)
        assertFalse(domain.isDeleted)
        assertNull(domain.deletedAt)

        val mappedEntity = ProductEntity.fromDomain(domain)
        assertEquals("kg", mappedEntity.unit)
        assertEquals(5, mappedEntity.lowStockThreshold)
        assertFalse(mappedEntity.isDeleted)
        assertNull(mappedEntity.deletedAt)
    }
}
