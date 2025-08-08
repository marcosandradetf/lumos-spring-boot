package com.lumos.api

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.lumos.R
import com.lumos.notifications.FCMService.FCMBus
import com.lumos.notifications.NotificationItem

object UserExperience {
    fun sendNotification(
        context: Context,
        title: String,
        body: String,
        intent: PendingIntent? = null,

        action: String = "",
        time: String = "",
        type: String = "",
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

        val notification = NotificationItem(
            title = title,
            body = body,
            action = action,
            time = time,
            type = type,
        )

        FCMBus.emit(notification)

        // Exibe a notificação
        notificationManager.notify(0, builder.build()) // 0 é o ID da notificação
    }

    fun vibrate(context: Context, duration: Long) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))

    }
}

object NotificationType {
    const val CONTRACT = "CONTRACT"
    const val UPDATE = "UPDATE"
    const val EVENT = "EVENT"
    const val WARNING = "WARNING"
    const val CASH = "CASH"
    const val ALERT = "ALERT"
    const val EXECUTION = "EXECUTION"
}
