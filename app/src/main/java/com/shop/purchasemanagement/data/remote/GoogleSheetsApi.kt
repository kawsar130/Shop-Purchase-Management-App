package com.shop.purchasemanagement.data.remote

import com.shop.purchasemanagement.data.model.ApiResponse
import com.shop.purchasemanagement.data.model.PostPayload
import com.shop.purchasemanagement.data.model.Product
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface GoogleSheetsApi {

    @GET
    suspend fun checkAuth(
        @Url url: String,
        @Query("email") email: String,
        @Query("action") action: String = "checkAuth"
    ): Response<ApiResponse<Any>>

    @GET
    suspend fun getProducts(
        @Url url: String,
        @Query("email") email: String,
        @Query("action") action: String = "getProducts"
    ): Response<ApiResponse<List<Product>>>

    @POST
    suspend fun postAction(
        @Url url: String,
        @Body payload: PostPayload
    ): Response<ApiResponse<Any>>
}
