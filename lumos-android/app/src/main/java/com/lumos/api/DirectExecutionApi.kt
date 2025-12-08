package com.lumos.api

import com.lumos.domain.model.DirectExecutionDTOResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface DirectExecutionApi {

    @GET("api/mobile/execution/get-direct-executions")
    suspend fun getDirectExecutions(
        @Query("uuid") uuid: String
    ): Response<List<DirectExecutionDTOResponse>>
    
    @Multipart
    @POST("api/mobile/execution/upload-direct-execution")
    suspend fun submitDirectExecutionStreet(
        @Part photo: MultipartBody.Part,
        @Part("execution") execution: RequestBody
    ): Response<Void>

    @Multipart
    @POST("api/mobile/v2/direct-execution/finish")
    suspend fun submitDirectExecution(
        @Part signature: MultipartBody.Part?,
        @Part("installation") installation: RequestBody?,
    ): Response<Void>
}