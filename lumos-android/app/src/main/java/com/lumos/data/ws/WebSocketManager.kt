package com.lumos.data.ws

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lumos.MainActivity
import com.lumos.R
import com.lumos.midleware.SecureStorage
import com.squareup.moshi.JsonClass
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent


@JsonClass(generateAdapter = true)
data class NotificationMessage(
    val title: String,
    val body: String,
    val action: String
)

class WebSocketManager(private val secureStorage: SecureStorage, private val context: Context) {
    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()

    fun startWebSocketConnection(userId: String) {
        Log.d("WebSocket", "🟢 Iniciando conexão WebSocket...")

        val token = secureStorage.getAccessToken()
        if (token.isNullOrEmpty()) {
            Log.e("WebSocket", "🔴 Token não encontrado ou inválido")
            return
        }

        // Criando os headers com o token JWT
        val headers = mapOf("Authorization" to "Bearer $token")

        // Criando o cliente STOMP com os headers
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://192.168.3.2:8080/ws")
        stompClient?.withClientHeartbeat(30000)?.withServerHeartbeat(30000)

        Log.d("WebSocket", "🔗 Tentando conectar ao WebSocket STOMP...")

        // Adicionando o listener do ciclo de vida da conexão
        compositeDisposable.add(stompClient!!.lifecycle().subscribe { event ->
            Log.d("WebSocket", "🔄 Evento recebido: ${event.type}")

            when (event.type!!) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("WebSocket", "✅ Conexão WebSocket aberta")
                    subscribeToTopic(userId)  // Chamando a inscrição no tópico
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d("WebSocket", "❌ Conexão WebSocket fechada")
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("WebSocket", "⚠️ Erro WebSocket: ${event.exception?.message}")
                }
                LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                    Log.e("WebSocket", "⚠️ FAILED_SERVER_HEARTBEAT: ${event.exception?.message}")
                }
            }
        })

        stompClient?.connect()

        // Timeout para verificar falha na conexão
        Handler(Looper.getMainLooper()).postDelayed({
            if (stompClient?.isConnected != true) {
                Log.e("WebSocket", "🚨 Timeout: conexão não estabelecida após 5 segundos")
            }
        }, 5000)
    }

    private fun subscribeToTopic(userId: String) {
        val topic = "/topic/$userId"
        Log.d("WebSocket", "📌 Tentando inscrever no tópico: $topic")

        val disposable: Disposable = stompClient!!.topic(topic)
            .subscribe({ message ->
                Log.d("WebSocket", "📩 Mensagem recebida do tópico $topic: ${message.payload}")
                handleIncomingNotification(message.payload)
            }, { error ->
                Log.e("WebSocket", "❌ Erro na inscrição ao tópico: ${error.message}")
            })

        compositeDisposable.add(disposable)
        Log.d("WebSocket", "✅ Inscrito no tópico: $topic")
    }

    private fun handleIncomingNotification(jsonMessage: String) {
        try {
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val adapter = moshi.adapter(NotificationMessage::class.java)
            val notification = adapter.fromJson(jsonMessage)

            println(notification)
            notification?.let { showNotification(it) }
        } catch (e: Exception) {
            Log.e("WebSocket", "Erro ao processar a notificação: ${e.message}", e)
        }
    }

    private fun showNotification(notification: NotificationMessage) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", notification.action)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "notifications_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notificações", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, builder)
    }

    fun stopWebSocketConnection() {
        stompClient?.disconnect()
        compositeDisposable.dispose()
    }
}
