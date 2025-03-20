package com.lumos.lumosspring.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
open class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic") // Permite enviar mensagens para clientes
        registry.setApplicationDestinationPrefixes("/app") // Prefixo para mensagens do cliente
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS() // Habilita WebSockets
    }
}