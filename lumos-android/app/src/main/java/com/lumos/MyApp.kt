package com.lumos

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.lumos.data.database.AppDatabase
import com.lumos.midleware.AuthInterceptor
import com.lumos.midleware.SecureStorage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MyApp : Application(), Application.ActivityLifecycleCallbacks {
    lateinit var database: AppDatabase
    lateinit var retrofit: Retrofit
    lateinit var secureStorage: SecureStorage
    private var currentActivity: Activity? = null


    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        createNotificationChannel()
        FirebaseApp.initializeApp(this)

//        // Inicializar WorkManager
//        WorkManager.initialize(this, Configuration.Builder().build())

        secureStorage = SecureStorage(this)

        // Inicializar Room
        database = AppDatabase.getInstance(this)

        // Inicializar Retrofit
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Timeout de conexão
            .writeTimeout(60, TimeUnit.SECONDS)   // Timeout de escrita
            .readTimeout(60, TimeUnit.SECONDS)    // Timeout de leitura
            .addInterceptor(AuthInterceptor(this, secureStorage))
            .build()

        retrofit = Retrofit.Builder()
//            .baseUrl("https://spring.thryon.com.br")
            .baseUrl("https://c1b07aa1dbca.ngrok-free.app")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()


    }

    fun getCurrentActivity(): Activity? = currentActivity

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "default_channel_id",
            "Canal Padrão",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal padrão para notificações do Firebase"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}
