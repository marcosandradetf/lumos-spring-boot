package com.lumos.lumosspring.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.concurrent.ConcurrentHashMap

//
//@Configuration
//@EnableWebSocketMessageBroker
//open class WebSocketConfig(private val jwtHandshakeInterceptor: JwtHandshakeInterceptor) : WebSocketMessageBrokerConfigurer {
//
//    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
//        registry.addEndpoint("/ws") // Endpoint WebSocket
////            .addInterceptors(jwtHandshakeInterceptor) // Adiciona o interceptor JWT
//            .setAllowedOrigins("*")
////            .withSockJS() // Compatibilidade com navegadores antigos
//    }
//
//    override fun configureMessageBroker(config: MessageBrokerRegistry) {
//        config.enableSimpleBroker("/topic")
//            .setHeartbeatValue(longArrayOf(30000, 30000)) // 30s cliente -> servidor e vice-versa
//        config.setApplicationDestinationPrefixes("/app")
//    }
//}

@Configuration
@EnableWebSocketMessageBroker
open class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic") // Prefix for broadcasting messages
        config.setApplicationDestinationPrefixes("/app") // Prefix for client-to-server communication
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws") // This is the WebSocket endpoint
            .setAllowedOrigins("*") // Allow frontend origin (localhost:63342)
            .withSockJS() // Enable SockJS for fallback support
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
        val token = request.headers.getFirst("Authorization")?.removePrefix("Bearer ")
        if (token.isNullOrEmpty()) {
            System.out.println("WebSocket: Token não encontrado ou inválido")
            return false
        }

        System.out.println("WebSocket: Token extraído: $token") // Adicione um log para verificar o token

        return try {
            val jwt = jwtDecoder.decode(token)
            val userId = jwt.subject

            val sessionId = attributes["sessionId"]?.toString() ?: userId

            sessionManager.addSession(sessionId, userId)
            attributes["userId"] = userId
            true
        } catch (e: JwtException) {
            System.out.println("WebSocket: Erro ao decodificar o token JWT $e")
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