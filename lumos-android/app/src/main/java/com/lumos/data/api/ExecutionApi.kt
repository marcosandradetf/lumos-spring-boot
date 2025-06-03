package com.lumos.data.api

import com.lumos.domain.model.Contract
import com.lumos.domain.model.ExecutionDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.UUID

interface ExecutionApi {
    @GET("/api/mobile/execution/get-executions")
    suspend fun getExecutions(
        @Query("uuid") uuid: String
    ): Response<List<ExecutionDTO>>

    @Multipart
    @POST("/api/mobile/execution/upload")
    suspend fun uploadData(
        @Part photo: MultipartBody.Part,
        @Part("execution") execution: RequestBody
    ): Response<Void>

}