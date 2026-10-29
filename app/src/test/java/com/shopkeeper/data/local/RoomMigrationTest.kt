package com.shopkeeper.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMigrationTest {

    @Test
    fun `MIGRATION_1_2 drops unique index and creates non-unique index on code`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlList = mutableListOf<String>()
        every { db.execSQL(capture(sqlList)) } just Runs

        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        AppDatabase.MIGRATION_1_2.migrate(db)

        assertTrue("Should drop existing index", sqlList.any { it.contains("DROP INDEX IF EXISTS index_products_code") })
        assertTrue("Should recreate non-unique index", sqlList.any { it.contains("CREATE INDEX IF NOT EXISTS index_products_code ON products(code)") })
    }

    @Test
    fun `MIGRATION_2_3 performs table rebuild converting REAL to INTEGER minor units`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlList = mutableListOf<String>()
        every { db.execSQL(capture(sqlList)) } just Runs

        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)

        AppDatabase.MIGRATION_2_3.migrate(db)

        // 1. Verify products_new table creation with INTEGER minor units
        val createTableSql = sqlList.firstOrNull { it.contains("CREATE TABLE IF NOT EXISTS products_new") }
        assertTrue("Must create temporary products_new table", createTableSql != null)
        assertTrue("products_new must have purchasePriceMinor INTEGER", createTableSql!!.contains("purchasePriceMinor INTEGER NOT NULL"))
        assertTrue("products_new must have salesPriceMinor INTEGER", createTableSql.contains("salesPriceMinor INTEGER NOT NULL"))

        // 2. Verify data migration with ROUND and CAST
        val insertSql = sqlList.firstOrNull { it.contains("INSERT INTO products_new") }
        assertTrue("Must copy rows into products_new", insertSql != null)
        assertTrue("Must convert purchasePrice to minor units", insertSql!!.contains("CAST(ROUND(purchasePrice * 100) AS INTEGER)"))
        assertTrue("Must convert salesPrice to minor units", insertSql.contains("CAST(ROUND(salesPrice * 100) AS INTEGER)"))

        // 3. Verify old table drop and rename
        assertTrue("Must drop old table", sqlList.any { it.trim() == "DROP TABLE products" })
        assertTrue("Must rename new table", sqlList.any { it.trim() == "ALTER TABLE products_new RENAME TO products" })

        // 4. Verify index recreation
        assertTrue("Must create index on code", sqlList.any { it.contains("index_products_code") })
        assertTrue("Must create index on name", sqlList.any { it.contains("index_products_name") })
        assertTrue("Must create index on category", sqlList.any { it.contains("index_products_category") })
    }

    @Test
    fun `MIGRATION_1_3 executes full migration chain from v1 to v3`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sqlList = mutableListOf<String>()
        every { db.execSQL(capture(sqlList)) } just Runs

        assertEquals(1, AppDatabase.MIGRATION_1_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_1_3.endVersion)

        AppDatabase.MIGRATION_1_3.migrate(db)

        // Verifies both v1->v2 and v2->v3 scripts ran
        assertTrue("Must run MIGRATION_1_2", sqlList.any { it.contains("DROP INDEX IF EXISTS index_products_code") })
        assertTrue("Must run MIGRATION_2_3", sqlList.any { it.contains("CREATE TABLE IF NOT EXISTS products_new") })
    }
}
