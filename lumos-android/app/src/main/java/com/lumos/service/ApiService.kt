package com.lumos.service

import com.lumos.data.entities.Measurement
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/measurements")
    suspend fun sendMeasurement(@Body measurement: Measurement)

}