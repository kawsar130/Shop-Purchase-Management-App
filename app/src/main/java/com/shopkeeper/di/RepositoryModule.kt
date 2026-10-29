package com.shopkeeper.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.shopkeeper.data.local.ProductDao
import com.shopkeeper.data.remote.FirestoreService
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import com.shopkeeper.data.sync.SyncScheduler
import com.shopkeeper.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserSessionRepository(@ApplicationContext context: Context): UserSessionRepository {
        return UserSessionRepository(context)
    }

    @Provides
    @Singleton
    fun provideProductRepository(
        productDao: ProductDao,
        firestoreService: FirestoreService,
        userSession: UserSessionRepository,
        networkMonitor: NetworkMonitor,
        syncScheduler: SyncScheduler,
        firebaseAuth: FirebaseAuth
    ): ProductRepository {
        return ProductRepository(
            productDao = productDao,
            firestoreService = firestoreService,
            userSession = userSession,
            networkMonitor = networkMonitor,
            syncScheduler = syncScheduler,
            firebaseAuth = firebaseAuth
        )
    }
}
