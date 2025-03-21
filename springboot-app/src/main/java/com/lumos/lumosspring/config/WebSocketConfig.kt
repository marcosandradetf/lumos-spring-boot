package com.lumos.lumosspring.config

import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.util.concurrent.ConcurrentHashMap

@Configuration
@EnableWebSocketMessageBroker
open class WebSocketConfig(private val jwtHandshakeInterceptor: JwtHandshakeInterceptor) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws") // Endpoint WebSocket
            .addInterceptors(jwtHandshakeInterceptor) // Adiciona o interceptor JWT
            .withSockJS() // Compatibilidade com navegadores antigos
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic") // Para envio de mensagens em tempo real
        registry.setApplicationDestinationPrefixes("/app") // Prefixo para requisições WebSocket
    }
}

@Component
class JwtHandshakeInterceptor(
    private val jwtDecoder: JwtDecoder,
    private val sessionManager: WebSocketSessionManager
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val token = request.headers.getFirst("Authorization")?.replace("Bearer ", "")

        return try {
            if (token != null) {
                val jwt = jwtDecoder.decode(token)
                val userId = jwt.subject

                // Obtém o sessionId do STOMP/WebSocket
                val sessionId = attributes["sessionId"]?.toString() ?: userId // Se não houver sessionId, usa o próprio userId

                // Armazena no SessionManager
                sessionManager.addSession(sessionId, userId)
                attributes["userId"] = userId
                true
            } else {
                false
            }
        } catch (e: JwtException) {
            false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        ex: Exception?
    ) {
        // Nada a fazer aqui
    }
}



@Component
class WebSocketSessionManager {

    // Mapeia sessionId -> userId
    private val sessionMap = ConcurrentHashMap<String, String>()

    fun addSession(sessionId: String, userId: String) {
        sessionMap[sessionId] = userId
    }

    fun removeSession(sessionId: String) {
        sessionMap.remove(sessionId)
    }

    fun getUserBySession(sessionId: String): String? {
        return sessionMap[sessionId]
    }
}