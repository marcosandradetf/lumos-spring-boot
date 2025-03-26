package com.lumos.data.api

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.lumos.R

object NotificationClient {
    fun sendNotification(
        context: Context,
        title: String,
        body: String,
        intent: PendingIntent? = null
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, "default_channel_id")
            .setSmallIcon(R.mipmap.ic_lumos) // Defina o ícone adequado
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Verifica se o PendingIntent foi passado e, se sim, define a ação de clique na notificação
        if (intent != null) {
            builder.setContentIntent(intent)
                .setAutoCancel(true) // A notificação será removida ao ser clicada
        } else {
            builder.setAutoCancel(true)
        }

        // Exibe a notificação
        notificationManager.notify(0, builder.build()) // 0 é o ID da notificação
    }
}
