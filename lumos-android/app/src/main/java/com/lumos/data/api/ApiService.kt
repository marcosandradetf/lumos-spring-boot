package com.lumos.data.api

import android.content.Context
import com.lumos.midleware.AuthInterceptor
import com.lumos.midleware.SecureStorage
import com.lumos.utils.ConnectivityUtils.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiService(
    private val context: Context,
    private val secureStorage: SecureStorage,
) {

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
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getOkHttpClient())
            .build()
    }

    // Cria instâncias das APIs
    fun <T> createApi(apiClass: Class<T>): T {
        return retrofit.create(apiClass)
    }
}


