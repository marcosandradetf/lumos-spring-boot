package com.lumos.lumosspring.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.lumos.lumosspring.user.model.UserStatus
import com.lumos.lumosspring.user.repository.UserRepository
import com.lumos.lumosspring.util.ApiErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class UserActivationAccessFilter(
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth !is JwtAuthenticationToken || !auth.isAuthenticated) {
            filterChain.doFilter(request, response)
            return
        }

        val userId = try {
            UUID.fromString(auth.name)
        } catch (_: IllegalArgumentException) {
            filterChain.doFilter(request, response)
            return
        }

        val user = userRepository.findByUserId(userId).orElse(null)
        if (user == null || user.status == UserStatus.ACTIVE) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            objectMapper.writeValueAsString(
                ApiErrorResponse(
                    error = "USER_NOT_ACTIVATED",
                    message = "User must complete activation"
                )
            )
        )
    }
}
