package com.lumos.api

import com.lumos.domain.model.MaintenanceStreetWithItems
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MaintenanceApi {

    @POST("api/maintenance/send-street")
    suspend fun submitMaintenanceStreet(
        @Body maintenanceStreetWithItems: MaintenanceStreetWithItems,
        @Query("teamId") teamId: Long?
    ): Response<Void>

    @Multipart
    @POST("api/maintenance/finish-maintenance")
    suspend fun submitMaintenance(
        @Part("maintenance") maintenance: RequestBody,
        @Part signature: MultipartBody.Part?,
    ): Response<Void>


}