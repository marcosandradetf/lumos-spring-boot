package com.lumos.domain.service

import android.content.Context
import android.location.Geocoder
import android.util.Log

class AddressService(private val context: Context) {
    fun execute(latitude: Double, longitude: Double): Array<String>? {
        return try {
            val geocoder = Geocoder(context)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses.isNullOrEmpty()) {
                return null
            }

            val address = addresses[0]
            val street = address.thoroughfare ?: "Desconhecido" // Rua
            val neighborhood = address.subLocality ?: "Desconhecido" // Bairro
            val city = address.subAdminArea ?: "Desconhecido" // Cidade
            val state = address.adminArea ?: "Desconhecido" // Estado

            val streetAndNeighborhood = "$street - $neighborhood, $city - $state".trim().replace(Regex(" - ,| , "), ", ")

//            arrayOf(streetAndNeighborhood, city, state)
            arrayOf(street, neighborhood, city, state)
        } catch (e: Exception) {
            null
        }
    }
}

