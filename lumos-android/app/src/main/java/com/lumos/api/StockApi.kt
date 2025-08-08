package com.lumos.api

import com.lumos.domain.model.OrderWithItems
import com.lumos.domain.model.StockResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StockApi {
    @GET("api/mobile/stock/get-truck-stock")
    suspend fun getStock(
        @Query("uuid") uuid: String
    ): Response<StockResponse>

    @POST("api/mobile/stock/send-order")
    suspend fun sendOrder(
        @Query("uuid") uuid: String,
        @Body order: OrderWithItems
    ): Response<Void>


}