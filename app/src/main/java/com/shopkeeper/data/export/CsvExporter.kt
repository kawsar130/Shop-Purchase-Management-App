package com.shopkeeper.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.shopkeeper.data.model.Product
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    /**
     * Pure data layer function: Generates a CSV export file without performing UI transitions or Activity launches.
     */
    fun generateCsvFile(
        cacheDir: File,
        shopName: String,
        currencySymbol: String,
        products: List<Product>
    ): Result<File> {
        return try {
            val exportDir = File(cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            } else {
                // Prune export files older than 24 hours
                exportDir.listFiles()?.forEach { oldFile ->
                    if (System.currentTimeMillis() - oldFile.lastModified() > 24 * 60 * 60 * 1000L) {
                        oldFile.delete()
                    }
                }
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val sanitizedShopName = shopName.replace("[^a-zA-Z0-9_-]".toRegex(), "_").trim('_')
            val cleanName = sanitizedShopName.ifEmpty { "Shop" }
            val fileName = "${cleanName}_Inventory_$timestamp.csv"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Header row: use clean ASCII/ISO currency codes to prevent Mojibake in mobile spreadsheet apps
                    val safeCurrencyHeader = getSafeCurrencyHeader(currencySymbol)
                    writer.write("Product Code,Product Name,Category,Buying Price ($safeCurrencyHeader),Selling Price ($safeCurrencyHeader),Quantity,Profit ($safeCurrencyHeader),Margin (%),Markup (%)\n")

                    // Data rows
                    for (p in products) {
                        val row = listOf(
                            escapeCsv(p.code),
                            escapeCsv(p.name),
                            escapeCsv(p.category),
                            String.format(Locale.US, "%.2f", p.purchasePrice),
                            String.format(Locale.US, "%.2f", p.salesPrice),
                            p.quantity.toString(),
                            String.format(Locale.US, "%.2f", p.profit),
                            String.format(Locale.US, "%.1f%%", p.profitMarginPercentage),
                            String.format(Locale.US, "%.1f%%", p.markupPercentage)
                        ).joinToString(",")

                        writer.write(row)
                        writer.write("\n")
                    }
                    writer.flush()
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves currency symbols to clean ASCII/ISO headers to prevent Mojibake
     * (e.g. 'à§³') in spreadsheet engines like Google Sheets on Android that default to ISO-8859-1.
     */
    fun getSafeCurrencyHeader(symbol: String): String {
        return when (symbol.trim()) {
            "৳" -> "BDT"
            "₹" -> "INR"
            "€" -> "EUR"
            "£" -> "GBP"
            "¥" -> "JPY"
            "$" -> "$"
            else -> {
                val clean = symbol.replace("[^a-zA-Z0-9$]".toRegex(), "").trim()
                if (clean.isNotEmpty()) clean else "BDT"
            }
        }
    }

    /**
     * UI helper: Builds the standard share Intent for a generated CSV export file.
     */
    fun createShareIntent(context: Context, file: File, shopName: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(file.lastModified()))

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv; charset=UTF-8"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "$shopName - Inventory Export ($timestamp)")
            putExtra(Intent.EXTRA_TEXT, "Attached is the latest product inventory export for $shopName.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, "Open / Share Inventory Spreadsheet")
    }

    /**
     * Backward-compatible convenience wrapper combining generation and Activity launch.
     */
    suspend fun exportAndShare(
        context: Context,
        shopName: String,
        currencySymbol: String,
        products: List<Product>
    ): Result<File> {
        val genResult = generateCsvFile(context.cacheDir, shopName, currencySymbol, products)
        if (genResult.isSuccess) {
            val file = genResult.getOrThrow()
            val chooser = createShareIntent(context, file, shopName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
        return genResult
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
