package com.lumos.lumosspring.config

import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class JwtHandshakeInterceptor : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: org.springframework.web.socket.WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val query = request.uri.query // Pega os parâmetros da URL

        if (query != null && query.contains("token=")) {
            val token = query.substringAfter("token=") // Pega o token JWT
            attributes["jwt"] = token // Salva o token nos atributos da sessão WebSocket
        }

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: org.springframework.web.socket.WebSocketHandler,
        exception: Exception?
    ) {
        // Não precisa implementar nada aqui
    }
}
