package com.lumos.domain.service

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AddressService(private val context: Context) {

    suspend fun execute(latitude: Double, longitude: Double): Array<String>? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("pt", "BR"))
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                if (addresses.isNullOrEmpty()) return@withContext null
                val address = addresses[0]

                val street = address.thoroughfare ?: return@withContext null
                val neighborhood = address.subLocality ?: return@withContext null
                val city = address.subAdminArea ?: return@withContext null
                val state = address.adminArea ?: ""

                arrayOf(street, neighborhood, city, state)

            } catch (e: Exception) {
                null
            }
        }
}

