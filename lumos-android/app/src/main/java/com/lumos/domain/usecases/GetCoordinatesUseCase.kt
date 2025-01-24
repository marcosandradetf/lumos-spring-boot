package com.lumos.domain.usecases

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient

class GetCoordinatesUseCase(
    private val context: Context,
    private val locationProvider: FusedLocationProviderClient
) {
    fun execute(callback: (Double?, Double?) -> Unit) {
        // Verifique se as permissões de localização foram concedidas
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Se as permissões não foram concedidas, retorne valores nulos
            callback(null, null)
            return
        }

        // Obtém a última localização conhecida
        locationProvider.lastLocation.addOnSuccessListener { location ->
            location?.let {
                callback(it.latitude, it.longitude)
            } ?: run {
                // Caso a localização seja nula (nenhuma localização recente disponível)
                callback(null, null)
            }
        }.addOnFailureListener {
            // Trate possíveis falhas na obtenção da localização
            callback(null, null)
        }
    }
}