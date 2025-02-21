package com.lumos.data.api

import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Item
import com.lumos.domain.model.Measurement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MeasurementApi {
    @POST("/api/mobile/execution/insert-measurement")
    suspend fun sendMeasurement(@Body measurement: MeasurementDto, @Header("UUID") uuid: String)

}

data class MeasurementDto(
    val measurement: Measurement,
    val items: List<Item>
)