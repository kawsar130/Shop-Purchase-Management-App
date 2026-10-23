package com.shopkeeper

import android.app.Application
import com.shopkeeper.data.local.AppDatabase
import com.shopkeeper.data.remote.FirestoreService
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository

class ShopApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var userSessionRepository: UserSessionRepository
        private set

    lateinit var firestoreService: FirestoreService
        private set

    lateinit var productRepository: ProductRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        userSessionRepository = UserSessionRepository(this)
        firestoreService = FirestoreService()
        productRepository = ProductRepository(
            productDao = database.productDao(),
            firestoreService = firestoreService,
            userSession = userSessionRepository
        )
    }
}
