package com.lumos.lumosspring.billing.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.lumos.lumosspring.billing.domain.SubscriptionStatus.CANCELED
import com.lumos.lumosspring.billing.domain.SubscriptionStatus.EXPIRED
import com.lumos.lumosspring.billing.domain.SubscriptionStatus.TRIAL
import com.lumos.lumosspring.billing.service.TenantBillingAccessService
import com.lumos.lumosspring.util.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Bloqueia chamadas autenticadas à API quando a assinatura do tenant está
 * EXPIRED, CANCELED ou TRIAL vencido ([com.lumos.lumosspring.billing.domain.SubscriptionStatus]).
 *
 * Rotas isentas: leitura de planos e status da própria assinatura (para a tela de renovação).
 */
@Component
class BillingSubscriptionAccessFilter(
    private val tenantBillingAccessService: TenantBillingAccessService,
    private val objectMapper: ObjectMapper,
    @Value("\${lumos.billing.enforce-access:true}") private val enforceAccess: Boolean,
    @Value("\${lumos.billing.access-exempt-paths:/api/billing/subscription-status,/api/billing/renew,/api/billing/upgrade,/api/billing/payment}") private val exemptPathsRaw: String,
) : OncePerRequestFilter() {

    private val pathMatcher = AntPathMatcher()
    private val exemptPatterns: List<String> by lazy {
        exemptPathsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!enforceAccess) return true
        val uri = request.requestURI
        return exemptPatterns.any { pathMatcher.match(it, uri) }
    }

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
        val tenantClaim = auth.token.getClaimAsString("tenant") ?: run {
            filterChain.doFilter(request, response)
            return
        }
        val tenantId = try {
            UUID.fromString(tenantClaim)
        } catch (_: IllegalArgumentException) {
            filterChain.doFilter(request, response)
            return
        }

        val allowed = tenantBillingAccessService.isTenantAccessAllowed(tenantId)
        if (allowed.first) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        data class Response (
            val code: String,
            val message: String
        )

        val body = when (allowed.second) {
            "CANCELED" -> Response(
                code = "SUBSCRIPTION_CANCELED",
                message = "Sua assinatura foi cancelada. Reative um plano para continuar utilizando o sistema."
            )

            "EXPIRED" -> Response(
                code = "SUBSCRIPTION_EXPIRED",
                message = "Sua assinatura expirou. Escolha um plano para retomar o acesso."
            )

            "TRIAL" -> Response(
                code = "TRIAL_EXPIRED",
                message = "Seu período de teste gratuito foi encerrado. Escolha um plano para continuar."
            )

            else -> Response(
                code = "SUBSCRIPTION_INACTIVE",
                message = "Assinatura indisponível: trial encerrado, plano cancelado ou expirado. Renove para continuar."
            )
        }

        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
