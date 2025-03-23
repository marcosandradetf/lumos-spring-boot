package com.lumos.service
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.PrimaryKey
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lumos.MyApp
import com.lumos.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Notification(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val action: String
)
class FCMService : FirebaseMessagingService() {
    companion object {
        val _actionState = MutableStateFlow<String?>(null)
        val actionState = _actionState.asStateFlow() // Expor como StateFlow
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCMService", "🔥 Notificação recebida: ${remoteMessage.data}")

        val title = remoteMessage.data["title"] ?: "Nova Notificação"
        val body = remoteMessage.data["body"] ?: ""
        val action = remoteMessage.data["action"] ?: ""

        Log.d("FCMService", "✅ Ação recebida: $action")

        // Atualiza o estado global para navegação
        _actionState.value = action

        // Exibir alerta na tela
//        showAlertOnScreen(title, body)

        // Enviar para a barra de notificações
        sendNotification(title, body)
    }


    private fun sendNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, "default_channel_id") // 🔥 Definindo o canal correto
            .setSmallIcon(R.mipmap.ic_lumos)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(0, builder.build())
    }


    private fun showAlertOnScreen(title: String, message: String) {
        // Obter a atividade atual
        val currentActivity = getCurrentActivity() ?: return

        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(currentActivity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun getCurrentActivity(): Activity? {
        return (application as? MyApp)?.getCurrentActivity()
    }

    private fun saveNotification(){

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Novo token FCM: $token")
    }

}
