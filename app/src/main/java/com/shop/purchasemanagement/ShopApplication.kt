package com.shop.purchasemanagement

import android.app.Application
import com.shop.purchasemanagement.data.local.AppDatabase
import com.shop.purchasemanagement.data.remote.GoogleDriveSheetsService
import com.shop.purchasemanagement.data.repository.ProductRepository
import com.shop.purchasemanagement.data.repository.UserSessionRepository

class ShopApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var userSessionRepository: UserSessionRepository
        private set

    lateinit var driveSheetsService: GoogleDriveSheetsService
        private set

    lateinit var productRepository: ProductRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        userSessionRepository = UserSessionRepository(this)
        driveSheetsService = GoogleDriveSheetsService(this)
        productRepository = ProductRepository(
            productDao = database.productDao(),
            driveSheetsService = driveSheetsService,
            userSession = userSessionRepository
        )
    }
}
