package com.shopkeeper.data.export

import com.shopkeeper.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CsvExporterTest {

    @Test
    fun `generateCsvFile creates valid CSV with headers and escaped rows`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_csv_export_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val sampleProducts = listOf(
            Product(
                id = "1",
                code = "101",
                name = "Organic Honey, 500g",
                category = "Groceries",
                purchasePriceMinor = 10000L,
                salesPriceMinor = 12500L,
                quantity = 15
            ),
            Product(
                id = "2",
                code = "102",
                name = "LED \"Super\" Bulb",
                category = "Electronics",
                purchasePriceMinor = 5000L,
                salesPriceMinor = 8000L,
                quantity = 25
            )
        )

        val result = CsvExporter.generateCsvFile(tempDir, "Test Shop", "$", sampleProducts)
        assertTrue(result.isSuccess)

        val file = result.getOrThrow()
        assertTrue(file.exists())
        assertTrue(file.name.endsWith(".csv"))

        val content = file.readText(Charsets.UTF_8)
        val rawBytes = file.readBytes()

        // 1. Verify file starts directly with "Product Code" and has NO UTF-8 BOM bytes
        assertTrue("File must start with Product Code without BOM prefix", content.startsWith("Product Code,"))
        val hasBom = rawBytes.size >= 3 && rawBytes[0] == 0xEF.toByte() && rawBytes[1] == 0xBB.toByte() && rawBytes[2] == 0xBF.toByte()
        assertTrue("File must not contain raw UTF-8 BOM", !hasBom)

        // 2. Verify header exists with Margin and Markup
        assertTrue(content.contains("Product Code,Product Name,Category,Buying Price ($),Selling Price ($),Quantity,Profit ($),Margin (%),Markup (%)"))

        // 3. Verify escaped product names with commas and quotes
        assertTrue(content.contains("\"Organic Honey, 500g\""))
        assertTrue(content.contains("\"LED \"\"Super\"\" Bulb\""))

        // 4. Verify formatted numeric values
        assertTrue(content.contains("100.00,125.00,15,25.00,20.0%,25.0%"))

        // Cleanup
        file.delete()
        tempDir.deleteRecursively()
    }

    @Test
    fun `generateCsvFile maps Unicode currency symbols to clean ISO codes to prevent Mojibake`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_csv_bengali_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val sampleProducts = listOf(
            Product(
                id = "1",
                code = "BT778",
                name = "Bluetooth Mouse",
                category = "Mouse",
                purchasePriceMinor = 140000L,
                salesPriceMinor = 180000L,
                quantity = 10
            )
        )

        // Test with Bengali Taka sign
        val result = CsvExporter.generateCsvFile(tempDir, "BD Electronics", "৳", sampleProducts)
        assertTrue(result.isSuccess)

        val file = result.getOrThrow()
        val content = file.readText(Charsets.UTF_8)

        // Verify clean BDT in header instead of raw Unicode ৳ that corrupts in Google Sheets
        assertTrue("Header must contain BDT", content.contains("Buying Price (BDT),Selling Price (BDT),Quantity,Profit (BDT)"))
        assertTrue("Row must contain formatted numbers", content.contains("1400.00,1800.00,10,400.00"))

        // Cleanup
        file.delete()
        tempDir.deleteRecursively()
    }
}
