package com.lumos.lumosspring.notifications.service

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(
    private val firebaseApp: FirebaseApp
) {
    fun sendNotificationForTopic(
        title: String,
        body: String,
        action: String? = null,
        notificationCode: String,
        time: Instant,
        type: String
    ) {
        // Criar a mensagem para o tópico
        val messageBuilder = Message.builder()
            .setTopic(notificationCode)  // Nome do tópico
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
            println("✅ Notificação enviada para o tópico $notificationCode com sucesso: $response")
        } catch (e: Exception) {
            println("❌ Erro ao enviar notificação: ${e.message}")
        }
    }

    fun updateTeam(userId: UUID, title: String, body: String) {
        // Criar a mensagem para o tópico
        val messageBuilder = Message.builder()
            .setTopic("UPDATE_$userId")  // Nome do tópico
            .putData("title", title)  // 🔹 Agora a notificação será tratada no onMessageReceived
            .putData("body", body)
            .putData("time", Instant.now().toString())
            .putData("type", "UPDATE")
            .putData("action", "UPDATE")

        val message = messageBuilder.build()

        // Enviar a notificação
        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("✅ Notificação enviada para o tópico UPDATE com sucesso: $response")
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
