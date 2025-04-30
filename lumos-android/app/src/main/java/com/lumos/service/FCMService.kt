package com.lumos.service
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lumos.MyApp
import com.lumos.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Entity(tableName = "notifications_items")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val action: String,
    val time: String,
    val type: String
)


object NotificationsBadge{
    val _notificationBadge = MutableStateFlow(0)
    val notificationBadge = _notificationBadge.asStateFlow() // Expor como StateFlow
}

class FCMService : FirebaseMessagingService() {
    companion object {
        val _notificationItem = MutableStateFlow<NotificationItem?>(null)
        val notificationItem = _notificationItem.asStateFlow() // Expor como StateFlow
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCMService", "🔥 Notificação recebida: ${remoteMessage.data}")

        val title = remoteMessage.data["title"] ?: "Nova Notificação"
        val body = remoteMessage.data["body"] ?: ""
        val action = remoteMessage.data["action"] ?: ""
        val time = remoteMessage.data["time"] ?: ""
        val type = remoteMessage.data["type"] ?: ""


        // Atualiza o estado global para navegação
        _notificationItem.value = NotificationItem(
            title = title,
            body = body,
            action = action,
            time = time,
            type = type
        )

        // Exibir alerta na tela
//        showAlertOnScreen(title, body)

        // Enviar para a barra de notificações
        sendNotification(title, body)
    }


    private fun sendNotification(title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
