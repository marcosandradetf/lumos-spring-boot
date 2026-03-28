package com.lumos.lumosspring.billing.service

import com.lumos.lumosspring.billing.domain.BillingCycle
import com.lumos.lumosspring.billing.domain.InvoiceStatus
import com.lumos.lumosspring.billing.domain.SubscriptionStatus
import com.lumos.lumosspring.billing.integration.PaymentGatewayPort
import com.lumos.lumosspring.billing.model.Subscription
import com.lumos.lumosspring.billing.repository.InvoiceRepository
import com.lumos.lumosspring.billing.repository.SubscriptionRepository
import com.lumos.lumosspring.plan.repository.PlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

data class PaymentProcessResult(
    val paymentProviderCustomerId: String,
    val paymentReference: String,
    val status: String,
    val invoiceId: UUID?,
)

@Service
class BillingSubscriptionCommandService(
    private val subscriptionRepository: SubscriptionRepository,
    private val planRepository: PlanRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentGatewayPort: PaymentGatewayPort,
) {

    /**
     * Reativa assinatura (após expiração, cancelamento ou inadimplência) ou converte trial em pago.
     * Atualiza snapshot de preço conforme o catálogo atual do plano escolhido.
     */
    @Transactional
    fun renewSubscription(
        tenantId: UUID,
        planName: String?,
        billingCycle: BillingCycle,
    ): Subscription {
        val sub = subscriptionRepository.findByTenantId(tenantId).orElseThrow {
            IllegalArgumentException("Nenhuma assinatura encontrada para este tenant.")
        }
        val st = SubscriptionStatus.fromCodeOrNull(sub.status)
            ?: throw IllegalStateException("Status de assinatura inválido.")
        if (st == SubscriptionStatus.ACTIVE) {
            throw IllegalStateException("Assinatura já está ativa. Use upgrade para mudar de plano.")
        }
        val planKey = planName?.trim()?.ifBlank { null } ?: sub.planName
            ?: throw IllegalArgumentException("Defina o plano (planName) ou associe um plano à assinatura.")
        val plan = planRepository.findById(planKey).orElseThrow {
            IllegalArgumentException("Plano não encontrado: $planKey")
        }
        if (plan.isActive != true) {
            throw IllegalStateException("Plano inativo: $planKey")
        }
        val monthly = plan.pricePerUserMonthly
            ?: throw IllegalStateException("Plano sem preço mensal: $planKey")
        val yearly = plan.pricePerUserYearly

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ym = YearMonth.from(LocalDate.now(ZoneOffset.UTC))
        val periodStart = ym.atDay(1)
        val periodEnd = ym.atEndOfMonth()

        sub.planName = planKey
        sub.billingCycle = billingCycle.code
        sub.status = SubscriptionStatus.ACTIVE.code
        sub.trialEndsAt = null
        sub.cancelAtPeriodEnd = false
        sub.canceledAt = null
        sub.currentPeriodStart = periodStart
        sub.currentPeriodEnd = periodEnd
        sub.pricePerUserMonthlySnapshot = monthly.setScale(2, RoundingMode.HALF_UP)
        sub.pricePerUserYearlySnapshot = yearly?.setScale(2, RoundingMode.HALF_UP)
        sub.updatedAt = now
        return subscriptionRepository.save(sub)
    }

    /**
     * Troca de plano (upgrade/downgrade lógico). Mantém assinatura ativa quando já estava ativa ou em trial.
     */
    @Transactional
    fun upgradeSubscription(tenantId: UUID, targetPlanName: String): Subscription {
        val key = targetPlanName.trim()
        if (key.isEmpty()) {
            throw IllegalArgumentException("targetPlanName é obrigatório.")
        }
        val sub = subscriptionRepository.findByTenantId(tenantId).orElseThrow {
            IllegalArgumentException("Nenhuma assinatura encontrada para este tenant.")
        }
        val st = SubscriptionStatus.fromCodeOrNull(sub.status)
            ?: throw IllegalStateException("Status de assinatura inválido.")
        if (st == SubscriptionStatus.CANCELED || st == SubscriptionStatus.EXPIRED) {
            throw IllegalStateException("Renove a assinatura (/api/billing/renew) antes de alterar o plano.")
        }
        val plan = planRepository.findById(key).orElseThrow {
            IllegalArgumentException("Plano não encontrado: $key")
        }
        if (plan.isActive != true) {
            throw IllegalStateException("Plano inativo: $key")
        }
        val monthly = plan.pricePerUserMonthly
            ?: throw IllegalStateException("Plano sem preço mensal: $key")
        val yearly = plan.pricePerUserYearly
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        sub.planName = key
        sub.pricePerUserMonthlySnapshot = monthly.setScale(2, RoundingMode.HALF_UP)
        sub.pricePerUserYearlySnapshot = yearly?.setScale(2, RoundingMode.HALF_UP)
        sub.updatedAt = now
        return subscriptionRepository.save(sub)
    }

    /**
     * Registro de pagamento (stub até Stripe/Pagar.me). Garante customer no gateway e, se [invoiceId] for enviado, marca fatura como paga.
     */
    @Transactional
    fun processPayment(
        tenantId: UUID,
        billingEmail: String,
        invoiceId: UUID?,
    ): PaymentProcessResult {
        val sub = subscriptionRepository.findByTenantId(tenantId).orElseThrow {
            IllegalArgumentException("Nenhuma assinatura encontrada para este tenant.")
        }
        val customerId = paymentGatewayPort.ensureCustomer(tenantId, billingEmail.trim())
        sub.paymentProviderCustomerId = customerId
        sub.paymentProvider = "noop"
        sub.updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
        subscriptionRepository.save(sub)

        var invId: UUID? = null
        if (invoiceId != null) {
            val inv = invoiceRepository.findById(invoiceId).orElseThrow {
                IllegalArgumentException("Fatura não encontrada: $invoiceId")
            }
            if (inv.tenantId != tenantId) {
                throw IllegalArgumentException("Fatura não pertence a este tenant.")
            }
            inv.status = InvoiceStatus.PAID.code
            inv.paidAt = OffsetDateTime.now(ZoneOffset.UTC)
            inv.externalPaymentId = "noop_${UUID.randomUUID()}"
            invoiceRepository.save(inv)
            invId = inv.invoiceId
        }

        val ref = "pay_${UUID.randomUUID()}"
        return PaymentProcessResult(
            paymentProviderCustomerId = customerId,
            paymentReference = ref,
            status = "SUCCEEDED",
            invoiceId = invId,
        )
    }
}
