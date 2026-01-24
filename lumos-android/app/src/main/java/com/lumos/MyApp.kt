package com.lumos

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.lumos.data.database.AppDatabase
import com.lumos.domain.service.ConnectivityGate
import com.lumos.midleware.AuthInterceptor
import com.lumos.midleware.SecureStorage
import com.lumos.utils.ConnectivityUtils.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MyApp : Application(), Application.ActivityLifecycleCallbacks {
    lateinit var database: AppDatabase
    lateinit var retrofit: Retrofit
    lateinit var secureStorage: SecureStorage
    private var currentActivity: Activity? = null

    lateinit var mainClient: OkHttpClient
    lateinit var pingClient: OkHttpClient
    lateinit var connectivityGate: ConnectivityGate
    lateinit var remoteConfigClient: OkHttpClient
    lateinit var remoteConfigRetrofit: Retrofit


    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        createNotificationChannel()
        FirebaseApp.initializeApp(this)

        secureStorage = SecureStorage(this)

        // Inicializar Room
        database = AppDatabase.getInstance(this)

        // Inicializar Client
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()


        mainClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Timeout de conexão
            .writeTimeout(60, TimeUnit.SECONDS)   // Timeout de escrita
            .readTimeout(60, TimeUnit.SECONDS)    // Timeout de leitura
            .addInterceptor(AuthInterceptor(secureStorage, refreshClient))
            .build()

        // Inicializar Retrofit
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(mainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //ping Client
        pingClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()

        connectivityGate = ConnectivityGate(pingClient)

        remoteConfigClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        remoteConfigRetrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(remoteConfigClient)
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
