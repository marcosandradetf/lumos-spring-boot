package com.lumos.lumosspring.notification.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.lumos.lumosspring.user.Role.Values
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class NotificationService {

    fun sendNotificationForRole(title: String, body: String, action: String, role: Values, time: Instant, type: String) {
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

}

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val HOME = "home"
    const val MENU = "menu"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val MEASUREMENT_HOME = "measurement-home"
    const val MEASUREMENT_SCREEN = "measurement-screen"
    const val CONTRACT_SCREEN = "contract-screen"

}