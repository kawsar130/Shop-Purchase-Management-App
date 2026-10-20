package com.shop.purchasemanagement.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.shop.purchasemanagement.data.model.Product
import com.shop.purchasemanagement.data.model.ShopInfo
import com.shop.purchasemanagement.data.model.StaffMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class GoogleDriveSheetsService(private val context: Context) {

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    private var driveService: Drive? = null
    private var sheetsService: Sheets? = null
    private var currentAccountEmail: String = ""

    companion object {
        val REQUIRED_SCOPES = listOf(
            DriveScopes.DRIVE_FILE,
            SheetsScopes.SPREADSHEETS
        )
        const val SHEET_PRODUCTS = "Products"
        const val SHEET_CONFIG = "ShopConfig"
        const val SUFFIX_FILE_NAME = " - Shop Inventory"
    }

    /**
     * Initializes Google API clients with authenticated Google account credentials
     */
    fun setupCredentials(account: GoogleSignInAccount) {
        currentAccountEmail = account.email ?: ""
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            REQUIRED_SCOPES
        ).apply {
            selectedAccount = account.account
        }

        driveService = Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("ShopPurchaseManagement")
            .build()

        sheetsService = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("ShopPurchaseManagement")
            .build()
    }

    /**
     * Creates a new Shop Spreadsheet in the user's Google Drive with Products & Config sheets.
     */
    suspend fun createShopSpreadsheet(
        shopName: String,
        currencySymbol: String
    ): Result<ShopInfo> = withContext(Dispatchers.IO) {
        val sheets = sheetsService ?: return@withContext Result.failure(Exception("Not signed in to Google"))
        val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            val title = "$shopName$SUFFIX_FILE_NAME"

            // 1. Define Spreadsheet with Products and ShopConfig sheets
            val newSpreadsheet = Spreadsheet().apply {
                properties = SpreadsheetProperties().setTitle(title)
                this.sheets = listOf(
                    Sheet().apply {
                        properties = SheetProperties().setTitle(SHEET_PRODUCTS).setSheetId(0)
                    },
                    Sheet().apply {
                        properties = SheetProperties().setTitle(SHEET_CONFIG).setSheetId(1)
                    }
                )
            }

            val created = sheets.spreadsheets().create(newSpreadsheet).execute()
            val spreadsheetId = created.spreadsheetId

            // 2. Populate Products Sheet with Headers and Initial Items
            val productHeaders = listOf(
                listOf("ID", "Product Code", "Product Name", "Category", "Purchase Price", "Sales Price", "Quantity", "Last Updated"),
                listOf("PROD_001", "P-101", "Miniket Rice 5kg", "Groceries", 360, 420, 25, System.currentTimeMillis().toString()),
                listOf("PROD_002", "P-102", "Soybean Cooking Oil 1L", "Groceries", 175, 195, 40, System.currentTimeMillis().toString()),
                listOf("PROD_003", "P-103", "LED Bulb 15W", "Electronics", 120, 160, 50, System.currentTimeMillis().toString()),
                listOf("PROD_004", "P-104", "Heavy Duty Extension Cord 3M", "Electronics", 280, 350, 15, System.currentTimeMillis().toString())
            )

            val prodBody = ValueRange().setValues(productHeaders)
            sheets.spreadsheets().values()
                .update(spreadsheetId, "$SHEET_PRODUCTS!A1:H5", prodBody)
                .setValueInputOption("USER_ENTERED")
                .execute()

            // 3. Populate ShopConfig Sheet
            val configValues = listOf(
                listOf("Key", "Value"),
                listOf("ShopName", shopName),
                listOf("Currency", currencySymbol),
                listOf("OwnerEmail", currentAccountEmail),
                listOf("CreatedAt", System.currentTimeMillis().toString())
            )

            val configBody = ValueRange().setValues(configValues)
            sheets.spreadsheets().values()
                .update(spreadsheetId, "$SHEET_CONFIG!A1:B5", configBody)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(
                ShopInfo(
                    spreadsheetId = spreadsheetId,
                    shopName = shopName,
                    currencySymbol = currencySymbol,
                    ownerEmail = currentAccountEmail,
                    isOwner = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Discovers any existing shop spreadsheets in the user's Drive or shared with them.
     */
    suspend fun findExistingShopSpreadsheets(): Result<List<ShopInfo>> = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            val query = "mimeType = 'application/vnd.google-apps.spreadsheet' and name contains '$SUFFIX_FILE_NAME' and trashed = false"
            val fileList = drive.files().list()
                .setQ(query)
                .setFields("files(id, name, owners)")
                .execute()

            val results = mutableListOf<ShopInfo>()
            for (file in fileList.files ?: emptyList()) {
                val name = file.name ?: ""
                val shopName = name.removeSuffix(SUFFIX_FILE_NAME).trim()
                val isOwner = file.owners?.any { it.emailAddress.equals(currentAccountEmail, ignoreCase = true) } ?: false
                val ownerEmail = file.owners?.firstOrNull()?.emailAddress ?: ""

                // Try reading currency from ShopConfig
                val currency = getCurrencyFromConfig(file.id) ?: "$"

                results.add(
                    ShopInfo(
                        spreadsheetId = file.id,
                        shopName = shopName.ifEmpty { "My Shop" },
                        currencySymbol = currency,
                        ownerEmail = ownerEmail,
                        isOwner = isOwner
                    )
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getCurrencyFromConfig(spreadsheetId: String): String? = withContext(Dispatchers.IO) {
        val sheets = sheetsService ?: return@withContext null
        try {
            val resp = sheets.spreadsheets().values().get(spreadsheetId, "$SHEET_CONFIG!A2:B5").execute()
            val rows = resp.getValues() ?: return@withContext null
            for (row in rows) {
                if (row.size >= 2 && (row[0] as? String).equals("Currency", ignoreCase = true)) {
                    return@withContext row[1]?.toString()
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Fetch all products from the spreadsheet.
     */
    suspend fun getProducts(spreadsheetId: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        val sheets = sheetsService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            val response = sheets.spreadsheets().values()
                .get(spreadsheetId, "$SHEET_PRODUCTS!A2:H")
                .execute()

            val rows = response.getValues() ?: emptyList()
            val products = mutableListOf<Product>()

            for (row in rows) {
                if (row.size < 3) continue // Skip incomplete rows
                val id = row.getOrNull(0)?.toString() ?: ""
                val code = row.getOrNull(1)?.toString() ?: ""
                val name = row.getOrNull(2)?.toString() ?: ""
                if (name.isEmpty() && code.isEmpty()) continue

                val category = row.getOrNull(3)?.toString() ?: "General"
                val purchasePrice = row.getOrNull(4)?.toString()?.toDoubleOrNull() ?: 0.0
                val salesPrice = row.getOrNull(5)?.toString()?.toDoubleOrNull() ?: 0.0
                val quantity = row.getOrNull(6)?.toString()?.toIntOrNull() ?: 0
                val updatedAt = row.getOrNull(7)?.toString() ?: System.currentTimeMillis().toString()

                products.add(
                    Product(
                        id = id.ifEmpty { "PROD_" + System.currentTimeMillis() },
                        code = code,
                        name = name,
                        category = category.ifEmpty { "General" },
                        purchasePrice = purchasePrice,
                        salesPrice = salesPrice,
                        quantity = quantity,
                        updatedAt = updatedAt
                    )
                )
            }

            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a product to the spreadsheet (appends to bottom row).
     */
    suspend fun appendProduct(spreadsheetId: String, product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        val sheets = sheetsService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            val row = listOf(
                product.id,
                product.code,
                product.name,
                product.category,
                product.purchasePrice,
                product.salesPrice,
                product.quantity,
                product.updatedAt
            )
            val body = ValueRange().setValues(listOf(row))
            sheets.spreadsheets().values()
                .append(spreadsheetId, "$SHEET_PRODUCTS!A:H", body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing product row in the spreadsheet.
     */
    suspend fun updateProduct(spreadsheetId: String, product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        val sheets = sheetsService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            // Find row index by matching ID or Code in columns A and B
            val range = "$SHEET_PRODUCTS!A2:B"
            val response = sheets.spreadsheets().values().get(spreadsheetId, range).execute()
            val rows = response.getValues() ?: emptyList()

            var targetRow = -1
            for (i in rows.indices) {
                val rowId = rows[i].getOrNull(0)?.toString() ?: ""
                val rowCode = rows[i].getOrNull(1)?.toString() ?: ""
                if (rowId == product.id || (product.code.isNotEmpty() && rowCode.equals(product.code, ignoreCase = true))) {
                    targetRow = i + 2 // 1-based + 1 header offset
                    break
                }
            }

            if (targetRow == -1) {
                // If not found, append as new
                return@withContext appendProduct(spreadsheetId, product)
            }

            val updatedRow = listOf(
                product.id,
                product.code,
                product.name,
                product.category,
                product.purchasePrice,
                product.salesPrice,
                product.quantity,
                product.updatedAt
            )

            val body = ValueRange().setValues(listOf(updatedRow))
            sheets.spreadsheets().values()
                .update(spreadsheetId, "$SHEET_PRODUCTS!A$targetRow:H$targetRow", body)
                .setValueInputOption("USER_ENTERED")
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a product row from the spreadsheet.
     */
    suspend fun deleteProduct(
        spreadsheetId: String,
        productId: String,
        productCode: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val sheets = sheetsService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            val range = "$SHEET_PRODUCTS!A2:B"
            val response = sheets.spreadsheets().values().get(spreadsheetId, range).execute()
            val rows = response.getValues() ?: emptyList()

            var targetRowIndex0Based = -1
            for (i in rows.indices) {
                val rowId = rows[i].getOrNull(0)?.toString() ?: ""
                val rowCode = rows[i].getOrNull(1)?.toString() ?: ""
                if (rowId == productId || (productCode.isNotEmpty() && rowCode.equals(productCode, ignoreCase = true))) {
                    targetRowIndex0Based = i + 1 // Row 1 is header (index 0), so Row 2 is index 1
                    break
                }
            }

            if (targetRowIndex0Based != -1) {
                val deleteReq = Request().setDeleteDimension(
                    DeleteDimensionRequest().setRange(
                        DimensionRange()
                            .setSheetId(0) // Products sheetId is 0
                            .setDimension("ROWS")
                            .setStartIndex(targetRowIndex0Based)
                            .setEndIndex(targetRowIndex0Based + 1)
                    )
                )

                val batchReq = BatchUpdateSpreadsheetRequest().setRequests(listOf(deleteReq))
                sheets.spreadsheets().batchUpdate(spreadsheetId, batchReq).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List all staff members shared on the spreadsheet.
     */
    suspend fun getStaffMembers(spreadsheetId: String): Result<List<StaffMember>> = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            val permissions = drive.permissions().list(spreadsheetId)
                .setFields("permissions(id, emailAddress, displayName, role)")
                .execute()

            val list = mutableListOf<StaffMember>()
            for (perm in permissions.permissions ?: emptyList()) {
                val email = perm.emailAddress ?: continue
                if (email.equals(currentAccountEmail, ignoreCase = true)) continue // Skip current user

                list.add(
                    StaffMember(
                        permissionId = perm.id ?: "",
                        email = email,
                        displayName = perm.displayName ?: email.substringBefore("@"),
                        role = if (perm.role == "owner") "Owner" else "Staff"
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Invite/Share the spreadsheet with a new staff member's Gmail.
     */
    suspend fun addStaffMember(spreadsheetId: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            val newPermission = Permission().apply {
                type = "user"
                role = "writer"
                emailAddress = email.trim()
            }

            drive.permissions().create(spreadsheetId, newPermission)
                .setSendNotificationEmail(true)
                .execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a staff member's access.
     */
    suspend fun removeStaffMember(spreadsheetId: String, permissionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext Result.failure(Exception("Not signed in to Google"))

        try {
            drive.permissions().delete(spreadsheetId, permissionId).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
