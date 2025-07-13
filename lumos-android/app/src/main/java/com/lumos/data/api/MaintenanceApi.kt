package com.lumos.data.api

import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceStreetWithItems
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MaintenanceApi {

    @POST("/api/maintenance/send-street")
    suspend fun sendStreet(
        @Body maintenanceStreetWithItems: MaintenanceStreetWithItems
    ): Response<Void>

    @POST("/api/maintenance/finish-maintenance")
    suspend fun finishMaintenance(
        @Body maintenance: Maintenance
    ): Response<Void>


}