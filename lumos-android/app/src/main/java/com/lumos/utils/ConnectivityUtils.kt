package com.lumos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ConnectivityUtils {
    suspend fun hasRealInternetConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://clients3.google.com/generate_204") // Não redireciona, resposta rápida
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun wifiConnected(context: Context) : Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isNetworkGood(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Check if Wi-Fi or Cellular
        try {
            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> true
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
                            level >= 2 // Pode ajustar o nível conforme necessário
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