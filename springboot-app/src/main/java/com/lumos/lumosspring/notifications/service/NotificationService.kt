package com.lumos.lumosspring.notifications.service

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.lumos.lumosspring.user.Role.Values
import com.lumos.lumosspring.util.NotificationType
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class NotificationService(
    private val firebaseApp: FirebaseApp
) {

    fun sendNotificationForRole(
        title: String,
        body: String,
        action: String,
        role: Values,
        time: Instant,
        type: String
    ) {
        // Criar a mensagem para o tópico
        val message = Message.builder()
            .setTopic(role.name)  // Nome do tópico
            .putData("title", title)  // 🔹 Agora a notificação será tratada no onMessageReceived
            .putData("body", body)
            .putData("action", action)
            .putData("time", time.toString())
            .putData("type", type)
            .build()

        // Enviar a notificação
        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("✅ Notificação enviada para o tópico ${role.name} com sucesso: $response")
        } catch (e: Exception) {
            println("❌ Erro ao enviar notificação: ${e.message}")
        }
    }

    fun sendNotificationForTeam(
        team: String,
        title: String,
        body: String,
        action: String,
        time: Instant,
        type: String,
        persistCode: String? = null
    ) {
        try {
            val ttl = when (type) {
                NotificationType.ALERT -> Duration.ofDays(7).toMillis()
                NotificationType.WARNING -> Duration.ofDays(7).toMillis()
                NotificationType.CONTRACT -> Duration.ofDays(1).toMillis()
                else -> Duration.ofHours(1).toMillis()
            }

            val androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH) // Garante entrega rápida
                .setTtl(ttl) // TTL: 7 dias (ajuste conforme sua lógica)
                .build()

            val messageBuilder = Message.builder()
                .setTopic(team)
                .setAndroidConfig(androidConfig)
                .putData("title", title)
                .putData("body", body)
                .putData("action", action)
                .putData("time", time.toString())
                .putData("type", type)

            persistCode?.let {
                messageBuilder.putData("persistCode", it)
            }

            val message = messageBuilder.build()

            val response = FirebaseMessaging.getInstance().send(message)
            println("✅ Notificação enviada para a equipe $team com sucesso: $response")

        } catch (e: Exception) {
            println("❌ Erro ao enviar notificação para a equipe $team: ${e.message}")
        }
    }

    fun sendNotificationForUserId(
        title: String,
        body: String,
        action: String? = null,
        userId: String,
        time: Instant,
        type: String
    ) {
        // Criar a mensagem para o tópico
        val messageBuilder = Message.builder()
            .setTopic(userId)  // Nome do tópico
            .putData("title", title)  // 🔹 Agora a notificação será tratada no onMessageReceived
            .putData("body", body)
            .putData("time", time.toString())
            .putData("type", type)

        action?.let {
            messageBuilder.putData("action", it)
        }

        val message = messageBuilder.build()

        // Enviar a notificação
        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("✅ Notificação enviada para o tópico $userId com sucesso: $response")
        } catch (e: Exception) {
            println("❌ Erro ao enviar notificação: ${e.message}")
        }
    }

}

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
    const val MENU = "menu"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CONTRACT_SCREEN = "contract-screen"
    const val PRE_MEASUREMENTS = "pre-measurements"
    const val PRE_MEASUREMENT_PROGRESS = "pre-measurement-progress"
    const val PRE_MEASUREMENT_STREET_HOME = "pre-measurement-home"
    const val PRE_MEASUREMENT_STREET = "pre-measurement-street"
    const val PRE_MEASUREMENT_STREET_PROGRESS = "pre-measurement-street"
    const val STOCK_CHECK = "stock-check"
}
