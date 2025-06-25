package com.lumos.data.api

import com.lumos.domain.model.Contract
import com.lumos.domain.model.DirectExecutionDTOResponse
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

    @GET("/api/mobile/execution/get-direct-executions")
    suspend fun getDirectExecutions(
        @Query("uuid") uuid: String
    ): Response<List<DirectExecutionDTOResponse>>

    @Multipart
    @POST("/api/mobile/execution/upload")
    suspend fun uploadData(
        @Part photo: MultipartBody.Part,
        @Part("execution") execution: RequestBody
    ): Response<Void>

    @Multipart
    @POST("/api/mobile/execution/upload-direct-execution")
    suspend fun uploadDirectExecutionData(
        @Part photo: MultipartBody.Part,
        @Part("execution") execution: RequestBody
    ): Response<Void>

    @POST("/api/mobile/execution/finish-direct-execution")
    suspend fun finishDirectExecution(
        @Query("contractId") contractId: Long
    ): Response<Void>

    @GET("/api/mobile/check-update")
    suspend fun checkUpdate(@Query("version") version: Long): Response<Update>

}

data class Update(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkUrl: String
)