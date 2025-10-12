package com.lumos.api

import com.lumos.domain.model.PreMeasurementInstallation
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PreMeasurementInstallationApi {
    @GET("api/mobile//v1/pre-measurement/installation/get-all/{status}")
    suspend fun getExecutions(@Path("status") status: String): Response<List<PreMeasurementInstallation>>

    @Multipart
    @POST("api/mobile/v1/pre-measurement/installation/save-street")
    suspend fun uploadData(
        @Part photo: MultipartBody.Part,
        @Part("execution") execution: RequestBody
    ): Response<Void>


    @GET("api/mobile/check-update")
    suspend fun checkUpdate(@Query("version") version: Long): Response<Update>

}

data class Update(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkUrl: String
)