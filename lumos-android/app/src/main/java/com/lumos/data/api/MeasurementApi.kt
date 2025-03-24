package com.lumos.data.api

import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreet
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MeasurementApi {
    @POST("/api/mobile/execution/insert-measurement")
    suspend fun sendMeasurement(@Body measurement: MeasurementDto, @Header("UUID") uuid: String)

}

data class MeasurementDto(
    val preMeasurementStreet: PreMeasurementStreet,
    val preMeasurementStreetItems: List<PreMeasurementStreetItem>
)