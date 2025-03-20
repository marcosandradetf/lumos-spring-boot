package com.lumos.lumosspring.notification.service

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class WebSocketNotificationService(private val messagingTemplate: SimpMessagingTemplate) {

    fun sendNotificationToRole(role: String, message: String) {
        messagingTemplate.convertAndSend("/topic/notifications/$role", message)
    }
}