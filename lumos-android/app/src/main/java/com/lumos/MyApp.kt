package com.lumos

import android.app.Application
import androidx.room.Room
import com.lumos.data.api.AuthApi
import com.lumos.data.database.AppDatabase
import com.lumos.midleware.AuthInterceptor
import com.lumos.midleware.SecureStorage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MyApp : Application() {
    lateinit var database: AppDatabase
    lateinit var retrofit: Retrofit
    lateinit var authApi: AuthApi
    lateinit var secureStorage: SecureStorage

    override fun onCreate() {
        super.onCreate()

//        // Inicializar WorkManager
//        WorkManager.initialize(this, Configuration.Builder().build())

        secureStorage = SecureStorage(this)

        // Inicializar Room
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()

        // Inicializar Retrofit
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Timeout de conexão
            .writeTimeout(60, TimeUnit.SECONDS)   // Timeout de escrita
            .readTimeout(60, TimeUnit.SECONDS)    // Timeout de leitura
            .addInterceptor(AuthInterceptor(secureStorage))
            .build()

        retrofit = Retrofit.Builder()
//            .baseUrl("https://spring.thryon.com.br")
            .baseUrl("http://192.168.3.2:8080")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()


    }
}
