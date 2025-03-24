package com.lumos.data.api

import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Material
import retrofit2.http.GET
import retrofit2.Response

interface StockApi {
    @GET("/api/mobile/stock/get-deposits")
    suspend fun getDeposits(): Response<List<Deposit>>

    @GET("/api/mobile/stock/get-items")
    suspend fun getMaterials(): Response<List<Material>>

}