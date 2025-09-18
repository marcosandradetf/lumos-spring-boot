package com.lumos.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ConnectivityUtils {
    const val BASE_URL = "https://79c42558c6b7.ngrok-free.app/"
//    const val BASE_URL = "https://api.thryon.com.br/spring/"
    private const val PING_URL = "https://api.thryon.com.br/spring/ping"

    suspend fun hasRealInternetConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(PING_URL) // Não redireciona, resposta rápida
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

}