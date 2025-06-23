package com.lumos.domain.service

import android.content.Context
import android.location.Geocoder
import android.util.Log
import java.util.Locale

class AddressService(private val context: Context) {
    fun execute(latitude: Double, longitude: Double): Array<String>? {
        return try {
            val geocoder = Geocoder(context, Locale("pt", "BR"))
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses.isNullOrEmpty()) {
                return null
            }

            val address = addresses[0]

            val street = address.thoroughfare ?: return null
            val neighborhood = address.subLocality ?: return null
            val city = address.subAdminArea ?: return null
            val state = address.adminArea ?: ""

//            val streetAndNeighborhood = "$street - $neighborhood, $city - $state".trim().replace(Regex(" - ,| , "), ", ")
//            arrayOf(streetAndNeighborhood, city, state)

            arrayOf(street, neighborhood, city, state)
        } catch (e: Exception) {
            null
        }
    }
}

