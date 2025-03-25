package com.lumos.data.api

import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreet
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MeasurementApi {
    @POST("/api/mobile/execution/insert-measurement")
    suspend fun sendMeasurement(@Body preMeasurementDto: PreMeasurementDto, @Header("UUID") uuid: String)

}

data class PreMeasurementDto(
    val contractId: Long,
    val streets: List<PreMeasurementStreetDto>,
)

data class PreMeasurementStreetDto(
    val street: PreMeasurementStreet,
    val items: List<PreMeasurementStreetItem>
)