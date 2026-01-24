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
    const val BASE_URL = "https://9e14dc82b25e.ngrok-free.app"
//    const val BASE_URL = "https://api.thryon.com.br/spring/"
    private const val PING_URL = "https://api.thryon.com.br/spring/ping"

}