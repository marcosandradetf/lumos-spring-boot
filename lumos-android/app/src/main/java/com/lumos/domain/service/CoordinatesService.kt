package com.lumos.domain.service

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.lumos.utils.Utils.hasLocationPermission
import com.lumos.utils.Utils.isLocationEnabled
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

class CoordinatesService(
    private val context: Context,
    private val locationProvider: FusedLocationProviderClient
) {

    private var previousLocation: Location? = null
    private var isLocationStable = false
    private var locationsHistory: MutableList<Location> = mutableListOf()
    private var executionCount = 0 // Contador de execuções

//    fun execute(callback: (Double?, Double?) -> Unit) {
//        // Verifique se as permissões foram concedidas
//        if (!hasLocationPermission(context) || !isLocationEnabled(context)) {
//            // Permissões ausentes, retorne valores nulos
//            Log.e("GET LOCATION", "Permissões de localização não concedidas.")
//            callback(null, null)
//            return
//        }
//
//        // Limpa a localização anterior e o estado de estabilidade
//        previousLocation = null
//        isLocationStable = false
//        executionCount = 0 // Reseta o contador
//        locationsHistory.clear()
//
//        // Configuração do LocationRequest com máxima precisão (combinando GPS, Wi-Fi e sensores)
//        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
//            .setWaitForAccurateLocation(true) // Aguarde pela melhor precisão possível
//            .setMaxUpdates(1) // Uma atualização por vez
//            .setMinUpdateDistanceMeters(0f) // Sem restrição de distância
//            .build()
//
//        // Listener para capturar a localização
//        val locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                while (executionCount < 3) {
//                    val location = locationResult.lastLocation
//                    location?.let {
//                        // Adiciona a localização ao histórico
//                        locationsHistory.add(it)
//                        if (locationsHistory.size > 5) {
//                            locationsHistory.removeAt(0) // Mantém no máximo 5 localizações no histórico
//                        }
//
//                        // Incrementa o contador de execuções
//                        executionCount++
//
//                        Log.e(
//                            "GET LOCATION",
//                            "Execução $executionCount: ${it.latitude}, ${it.longitude}"
//                        )
//
//                        // Verifica se alcançamos três execuções
//                        if (executionCount >= 3) {
//                            val stable = isLocationStable(it) // Verifica se a localização é estável
//                            Log.e("GET LOCATION", "Localização estável: $stable")
//
//                            // Após três execuções, retorna a média das localizações
//                            val avgLat = locationsHistory.map { loc -> loc.latitude }.average()
//                            val avgLon = locationsHistory.map { loc -> loc.longitude }.average()
//
//                            callback(avgLat, avgLon)
//                            locationProvider.removeLocationUpdates(this) // Remove o listener
//                        }
//
//                    } ?: run {
//                        Log.e("GET LOCATION", "Localização é nula.")
//                        callback(null, null)
//                    }
//                }
//            }
//        }
//
//        // Inicia a primeira solicitação de localização
//        try {
//            locationProvider.requestLocationUpdates(
//                locationRequest,
//                locationCallback,
//                Looper.getMainLooper()
//            )
//        } catch (se: SecurityException) {
//            Log.e("GET LOCATION", "Falha ao requisitar updates de localização", se)
//            callback(null, null)
//        }
//    }

    suspend fun execute(): Pair<Double?, Double?> =
        suspendCancellableCoroutine { cont ->

            if (!hasLocationPermission(context) || !isLocationEnabled(context)) {
                Log.e("GET LOCATION", "Permissões de localização não concedidas.")
                cont.resume(null to null)
                return@suspendCancellableCoroutine
            }

            previousLocation = null
            isLocationStable = false
            executionCount = 0
            locationsHistory.clear()

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000
            )
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(1)
                .setMinUpdateDistanceMeters(0f)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    while (executionCount < 3) {
                        val location = locationResult.lastLocation

                        location?.let {
                            locationsHistory.add(it)
                            if (locationsHistory.size > 5) {
                                locationsHistory.removeAt(0)
                            }

                            executionCount++

                            if (executionCount >= 3) {
                                val avgLat = locationsHistory.map { loc -> loc.latitude }.average()
                                val avgLon = locationsHistory.map { loc -> loc.longitude }.average()

                                cont.resume(avgLat to avgLon)
                                locationProvider.removeLocationUpdates(this)
                            }

                        } ?: run {
                            cont.resume(null to null)
                            locationProvider.removeLocationUpdates(this)
                        }
                    }
                }
            }

            cont.invokeOnCancellation {
                locationProvider.removeLocationUpdates(locationCallback)
            }

            try {
                locationProvider.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (se: SecurityException) {
                Log.e("GET LOCATION", "Falha ao requisitar updates de localização", se)
                cont.resume(null to null)
            }
        }


    private fun isLocationStable(location: Location): Boolean {
        // Calcula a média das localizações armazenadas no histórico
        val avgLat = locationsHistory.map { it.latitude }.average()
        val avgLon = locationsHistory.map { it.longitude }.average()

        // Verifica se a diferença entre a média e a última localização é pequena
        val latDiff = abs(avgLat - location.latitude)
        val lonDiff = abs(avgLon - location.longitude)

        // Ajuste o limite conforme necessário (o valor pode ser ajustado dependendo da precisão desejada)
        return latDiff < 0.0001 && lonDiff < 0.0001
    }
}
