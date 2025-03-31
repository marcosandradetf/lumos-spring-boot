package com.lumos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat

object ConnectivityUtils {
    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isNetworkGood(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Check if Wi-Fi or Cellular
        try {
            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    // Verificar o tipo de rede
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val networkType = telephonyManager.dataNetworkType

                    // Verifica se a rede é 4G ou superior
                    when (networkType) {
                        TelephonyManager.NETWORK_TYPE_LTE, // 4G
                        TelephonyManager.NETWORK_TYPE_NR -> { // 5G
                            // Verificar intensidade do sinal para redes móveis
                            val signalStrength = telephonyManager.signalStrength
                            val level = signalStrength?.level ?: 0

                            // Condição para 4G (LTE) ou superior
                            level >= 3 // Pode ajustar o nível conforme necessário
                        }

                        else -> false
                    }
                }

                else -> false
            }
        } catch (e: SecurityException) {
            // Caso a permissão não tenha sido concedida, trate a exceção
            Log.e("PermissionError", "Permissão negada para acessar o estado da rede.")
            return false
        }
    }
}