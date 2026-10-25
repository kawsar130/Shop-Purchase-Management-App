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

    fun exportAndShare(
        context: Context,
        shopName: String,
        currencySymbol: String,
        products: List<Product>
    ): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports")
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
                // Write UTF-8 BOM so Excel on Windows & mobile correctly parses Unicode characters & currency
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Header row
                    writer.write("Product Code,Product Name,Category,Buying Price ($currencySymbol),Selling Price ($currencySymbol),Quantity,Profit ($currencySymbol),Margin (%)\n")

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
                            String.format(Locale.US, "%.1f%%", p.profitMarginPercentage)
                        ).joinToString(",")

                        writer.write(row)
                        writer.write("\n")
                    }
                    writer.flush()
                }
            }

            // Launch system share chooser
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "$shopName - Inventory Export ($timestamp)")
                putExtra(Intent.EXTRA_TEXT, "Attached is the latest product inventory export for $shopName.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Open / Share Inventory Spreadsheet").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
