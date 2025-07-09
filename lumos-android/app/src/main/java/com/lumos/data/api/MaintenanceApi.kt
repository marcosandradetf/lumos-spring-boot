package com.lumos.data.api

import com.lumos.domain.model.DirectExecutionDTOResponse
import com.lumos.domain.model.ExecutionDTO
import com.lumos.domain.model.MaterialStock
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MaintenanceApi {
    @GET("/api/mobile/maintenance/get-stock")
    suspend fun getStock(
        @Query("uuid") uuid: String
    ): Response<List<MaterialStock>>

}