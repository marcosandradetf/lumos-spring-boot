package com.lumos.data.api

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.lumos.midleware.AuthInterceptor
import com.lumos.midleware.SecureStorage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiService(
    private val context: Context,
    private val secureStorage: SecureStorage,
) {
    private val baseUrl = "https://b8b9-2804-d45-365c-c100-253e-1174-61ea-e6fe.ngrok-free.app"
//    private val baseUrl = "http://192.168.2.13:8080"
//    private val baseUrl = "https://spring.thryon.com.br"
//    val apiKey = BuildConfig.API_URL


    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Timeout de conexão
            .writeTimeout(60, TimeUnit.SECONDS)   // Timeout de escrita
            .readTimeout(60, TimeUnit.SECONDS)    // Timeout de leitura
            .addInterceptor(AuthInterceptor(context, secureStorage))
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getOkHttpClient())
            .build()
    }

    // Cria instâncias das APIs
    fun <T> createApi(apiClass: Class<T>): T {
        return retrofit.create(apiClass)
    }
}


