package com.lumos.lumosspring.billing.controller

import com.lumos.lumosspring.billing.domain.BillingCycle
import com.lumos.lumosspring.billing.domain.SubscriptionStatus
import com.lumos.lumosspring.billing.repository.SubscriptionRepository
import com.lumos.lumosspring.billing.service.BillingSubscriptionCommandService
import com.lumos.lumosspring.billing.service.TenantBillingAccessService
import com.lumos.lumosspring.util.ErrorResponse
import com.lumos.lumosspring.util.Utils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

data class SubscriptionStatusResponse(
    val planName: String?,
    val status: String?,
    val trialEndsAt: OffsetDateTime?,
    val accessAllowed: Boolean,
    val message: String?,
)

data class RenewRequest(
    /** Se null, mantém o plano atual da assinatura. */
    val planName: String? = null,
    val billingCycle: String = BillingCycle.MONTHLY.code,
)

data class UpgradeRequest(
    val targetPlanName: String,
)

data class PaymentRequest(
    val billingEmail: String? = null,
    val invoiceId: UUID? = null,
)

data class SubscriptionMutationResponse(
    val subscriptionId: UUID?,
    val planName: String?,
    val status: String?,
    val message: String,
)

/**
 * Rotas de leitura e de comando de assinatura. Paths de comando ficam isentos do filtro de billing
 * para permitir renovação/upgrade com JWT mesmo quando o acesso ao restante da API está bloqueado.
 */
@RestController
@RequestMapping("/api/billing")
class BillingSubscriptionController(
    private val subscriptionRepository: SubscriptionRepository,
    private val tenantBillingAccessService: TenantBillingAccessService,
    private val billingSubscriptionCommandService: BillingSubscriptionCommandService,
) {

    @GetMapping("/subscription-status")
    fun subscriptionStatus(): SubscriptionStatusResponse {
        val tenantId = Utils.getCurrentTenantId()
        val sub = subscriptionRepository.findByTenantId(tenantId).orElse(null)
        val allowed = tenantBillingAccessService.isTenantAccessAllowed(tenantId).first
        val st = sub?.status?.let { SubscriptionStatus.fromCodeOrNull(it) }
        return SubscriptionStatusResponse(
            planName = sub?.planName,
            status = sub?.status,
            trialEndsAt = sub?.trialEndsAt,
            accessAllowed = allowed,
            message = when {
                allowed -> null
                sub == null && !allowed -> "Nenhuma assinatura cadastrada para este tenant."
                else -> st?.accessDeniedReason() ?: "Acesso não disponível no momento."
            },
        )
    }

    @PostMapping("/renew")
    fun renew(@RequestBody body: RenewRequest): ResponseEntity<*> {
        return try {
            val tenantId = Utils.getCurrentTenantId()
            val cycle = BillingCycle.fromCode(body.billingCycle)
            val sub = billingSubscriptionCommandService.renewSubscription(tenantId, body.planName, cycle)
            ResponseEntity.ok(
                SubscriptionMutationResponse(
                    subscriptionId = sub.subscriptionId,
                    planName = sub.planName,
                    status = sub.status,
                    message = "Assinatura renovada com sucesso.",
                ),
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "Requisição inválida."))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message ?: "Não foi possível renovar."))
        }
    }

    @PostMapping("/upgrade")
    fun upgrade(@RequestBody body: UpgradeRequest): ResponseEntity<*> {
        return try {
            val tenantId = Utils.getCurrentTenantId()
            val sub = billingSubscriptionCommandService.upgradeSubscription(tenantId, body.targetPlanName)
            ResponseEntity.ok(
                SubscriptionMutationResponse(
                    subscriptionId = sub.subscriptionId,
                    planName = sub.planName,
                    status = sub.status,
                    message = "Plano atualizado com sucesso.",
                ),
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "Requisição inválida."))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message ?: "Não foi possível alterar o plano."))
        }
    }

    @PostMapping("/payment")
    fun payment(@RequestBody body: PaymentRequest): ResponseEntity<*> {
        return try {
            val tenantId = Utils.getCurrentTenantId()
            val email = resolveBillingEmail(body.billingEmail)
                ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse("Informe billingEmail ou use token com claim 'email'."))
            val result = billingSubscriptionCommandService.processPayment(tenantId, email, body.invoiceId)
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "Requisição inválida."))
        }
    }

    private fun resolveBillingEmail(fromBody: String?): String? {
        val trimmed = fromBody?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed != null) return trimmed
        val auth = SecurityContextHolder.getContext().authentication
        if (auth is JwtAuthenticationToken) {
            return auth.token.getClaimAsString("email")
        }
        return null
    }
}
