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
            val street = address.thoroughfare ?: "" // Rua
            val neighborhood = address.subLocality ?: "" // Bairro
            val city = address.subAdminArea ?: "" // Cidade
            val state = address.adminArea ?: "" // Estado

            val logradouro = "$street - $neighborhood, $city - $state".trim().replace(Regex(" - ,| , "), ", ")
            val cidade = address.locality ?: "Desconhecida"         // Cidade
            val estado = address.adminArea ?: "Desconhecido"        // Estado


            arrayOf(logradouro, cidade, estado)
        } catch (e: Exception) {
            null
        }
    }
}

