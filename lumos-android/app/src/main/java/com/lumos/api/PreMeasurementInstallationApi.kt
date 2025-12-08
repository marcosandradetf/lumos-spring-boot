package com.lumos.api

import com.lumos.domain.model.InstallationResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PreMeasurementInstallationApi {
    @GET("api/mobile/v1/pre-measurement/installation/get-all/{status}")
    suspend fun getInstallations(@Path("status") status: String): Response<List<InstallationResponse>>

    @Multipart
    @POST("api/mobile/v1/pre-measurement/installation/save-street")
    suspend fun submitInstallationStreet(
        @Part photo: MultipartBody.Part?,
        @Part("installationStreet") installationStreet: RequestBody
    ): Response<Void>

    @Multipart
    @POST("api/mobile/v1/pre-measurement/installation/save-installation")
    suspend fun submitInstallation(
        @Part signature: MultipartBody.Part?,
        @Part("installation") installation: RequestBody
    ): Response<Void>
}