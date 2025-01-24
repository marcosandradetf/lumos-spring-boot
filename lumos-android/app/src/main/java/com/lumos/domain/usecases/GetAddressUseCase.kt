package com.lumos.domain.usecases

import android.content.Context
import android.location.Geocoder

class GetAddressUseCase(private val context: Context) {
    fun execute(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.get(0)?.getAddressLine(0)
        } catch (e: Exception) {
            null
        }
    }
}
