package com.lumos.lumosspring.config


import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class JwtChannelInterceptor(private val jwtDecoder: JwtDecoder) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        val token = accessor.getFirstNativeHeader("Authorization")?.replace("Bearer ", "")

        if (token != null) {
            try {
                // Decodificando o token JWT
                val jwt = jwtDecoder.decode(token)

                // Extraindo o nome do usuário e as authorities do JWT
                val username = jwt.subject
                val authorities = jwt.claims["authorities"] as List<*>

                // Criando o objeto Authentication para configurar no SecurityContext
                val auth = UsernamePasswordAuthenticationToken(username, null, authorities.map {
                    SimpleGrantedAuthority(it.toString())
                })
                SecurityContextHolder.getContext().authentication = auth
                accessor.user = auth
            } catch (e: JwtException) {
                // Se o token for inválido ou não puder ser decodificado, você pode lançar uma exceção ou fazer o tratamento necessário
                throw IllegalArgumentException("Invalid JWT token", e)
            }
        }

        return message
    }
}
