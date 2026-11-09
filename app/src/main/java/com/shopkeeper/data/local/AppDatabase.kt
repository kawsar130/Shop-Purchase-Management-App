package com.shopkeeper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 had a unique constraint on 'code'.
                // Version 2 allows duplicate 'code' for soft-deleted items (__del__ prefix).
                db.execSQL("DROP INDEX IF EXISTS index_products_code")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_code ON products(code)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migrate Double floating-point prices to exact Long minor units (cents/paisa).
                // 1. Create temporary products table with INTEGER minor units
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS products_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        purchasePriceMinor INTEGER NOT NULL,
                        salesPriceMinor INTEGER NOT NULL,
                        quantity INTEGER NOT NULL,
                        updatedAt TEXT NOT NULL,
                        isPendingSync INTEGER NOT NULL,
                        isDeletedLocally INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. Transfer existing data converting REAL to INTEGER cents
                db.execSQL("""
                    INSERT INTO products_new (
                        id, code, name, category, purchasePriceMinor, salesPriceMinor, quantity, updatedAt, isPendingSync, isDeletedLocally
                    )
                    SELECT 
                        id, code, name, category,
                        CAST(ROUND(purchasePrice * 100) AS INTEGER),
                        CAST(ROUND(salesPrice * 100) AS INTEGER),
                        quantity, updatedAt, isPendingSync, isDeletedLocally
                    FROM products
                """.trimIndent())

                // 3. Drop old table and rename new table
                db.execSQL("DROP TABLE products")
                db.execSQL("ALTER TABLE products_new RENAME TO products")

                // 4. Recreate indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_code ON products(code)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_name ON products(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_category ON products(category)")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT 'pcs'")
                db.execSQL("ALTER TABLE products ADD COLUMN lowStockThreshold INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shop_management.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
