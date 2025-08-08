package com.lumos.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UtilApi {
    @POST("api/mobile/util/generic/set-entity")
    suspend fun updateEntity(@Body preMeasurementDto: UpdateEntity):  Response<Void>

}

data class UpdateEntity(
    val table: String,
    val field: String,
    val set: Any,
    val where: String,
    val equal: Any
)
