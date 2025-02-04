package com.lumos.data.api

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.lumos.midleware.AuthInterceptor
import com.lumos.midleware.SecureStorage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiService(
    private val secureStorage: SecureStorage,
) {
//    private val baseUrl = "http://192.168.3.2:8080"
    private val baseUrl = "https://spring.thryon.com.br"

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(secureStorage))
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


