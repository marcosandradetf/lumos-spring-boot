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
        time: Instant = Instant.now(),
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
    const val AUTH_FLOW = "auth-flow"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
    const val NO_ACCESS = "no-access"
    const val MORE = "more"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CONTRACT_SCREEN = "contract-screen"
    const val PRE_MEASUREMENT_FLOW = "pre-measurement-flow"
    const val PRE_MEASUREMENTS = "pre-measurements"
    const val PRE_MEASUREMENT_PROGRESS = "pre-measurement-progress"
    const val PRE_MEASUREMENT_STREET = "pre-measurement-street"
    const val INSTALLATION_HOLDER = "installation-holder-screen"
    const val MAINTENANCE = "maintenance"
    const val STOCK = "stock"
    const val ORDER = "order"

    // -> pre-measurement-installations
    const val PRE_MEASUREMENT_INSTALLATION_FLOW = "pre-measurement-installation-flow"
    const val PRE_MEASUREMENT_INSTALLATION_STREETS = "pre-measurement-installation-streets"
    const val PRE_MEASUREMENT_INSTALLATION_MATERIALS = "pre-measurement-installation-materials"

    // -> direct-installations
    const val DIRECT_EXECUTION_FLOW = "direct-execution-flow"
    const val DIRECT_EXECUTION_HOME_SCREEN = "direct-execution-home-screen"
    const val DIRECT_EXECUTION_SCREEN_MATERIALS = "direct-execution-screen-materials"
    const val UPDATE = "update"
    const val SYNC_FLOW = "sync-flow"
    const val SYNC = "sync"
    const val TEAM_SCREEN = "team-screen"
}

object NotificationType {
    const val CONTRACT = "CONTRACT"
    const val UPDATE = "UPDATE"
    const val EVENT = "EVENT"
    const val WARNING = "WARNING"
    const val CASH = "CASH"
    const val ALERT = "ALERT"
    const val EXECUTION = "EXECUTION"
    const val CHANGE_TEAM = "CHANGE_TEAM"
}