package com.lumos.data.api

import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PreMeasurementApi {
    @POST("api/mobile/execution/insert-pre-measurement")
    suspend fun sendPreMeasurement(@Body preMeasurementDto: PreMeasurementDto, @Header("UUID") uuid: String):  Response<Void>

    @Multipart
    @POST("api/mobile/pre-measurement-street/upload-photos")
    suspend fun uploadStreetPhotos(
        @Part photos: List<MultipartBody.Part>,
    ): Response<Void>

}

data class PreMeasurementDto(
    val contractId: Long,
    val streets: List<PreMeasurementStreetDto>,
)

data class PreMeasurementStreetDto(
    val street: PreMeasurementStreet,
    val items: List<PreMeasurementStreetItem>
)