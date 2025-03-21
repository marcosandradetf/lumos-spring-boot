package com.lumos.lumosspring.notification.service

import com.lumos.lumosspring.config.WebSocketSessionManager
import kotlinx.coroutines.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

data class NotificationMessage(
    val title: String,
    val body: String,
    val action: String // Ex: "open_contracts"
)

@Service
class NotificationService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val sessionManager: WebSocketSessionManager,
    private val objectMapper: ObjectMapper  // Para converter para JSON
) {

    suspend fun sendNotificationToMultipleUsersAsync(userIds: List<UUID>, title: String, body: String, action: String) = coroutineScope {
        userIds.map { userId ->
            async { sendNotificationToUser(userId.toString(), title, body, action) }
        }.awaitAll()
    }

    private fun sendNotificationToUser(userId: String, title: String, body: String, action: String) {
        val sessionId = sessionManager.getUserBySession(userId)
        sessionId?.let {
            val notification = NotificationMessage(title, body, action)
            val jsonMessage = objectMapper.writeValueAsString(notification) // Converte para JSON
            messagingTemplate.convertAndSend("/topic/$userId", jsonMessage)
        }
    }
}
