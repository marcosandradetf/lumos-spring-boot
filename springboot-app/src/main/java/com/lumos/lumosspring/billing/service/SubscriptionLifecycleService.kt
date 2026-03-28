package com.lumos.lumosspring.billing.service

import com.lumos.lumosspring.billing.domain.BillingCycle
import com.lumos.lumosspring.billing.domain.SubscriptionStatus
import com.lumos.lumosspring.billing.model.Subscription
import com.lumos.lumosspring.billing.repository.SubscriptionRepository
import com.lumos.lumosspring.plan.repository.PlanRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/**
 * Onboarding com trial e expiração automática (job diário).
 * Snapshot de preço copiado do plano no momento da criação (evita cobrança errada se o preço mudar).
 */
@Service
class SubscriptionLifecycleService(
    private val subscriptionRepository: SubscriptionRepository,
    private val planRepository: PlanRepository,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    @Value("\${lumos.billing.trial-days:14}") private val trialDays: Long,
) {

    /**
     * Cria uma subscription em TRIAL para o tenant (chamar após criar tenant no onboarding).
     * Garante uma linha por tenant via UNIQUE(tenant_id) + verificação aqui.
     */
    @Transactional
    fun createTrialSubscription(tenantId: UUID, planName: String): Subscription {
        if (subscriptionRepository.findByTenantId(tenantId).isPresent) {
            throw IllegalStateException("Tenant já possui subscription")
        }
        val plan = planRepository.findById(planName).orElseThrow {
            IllegalArgumentException("Plano não encontrado: $planName")
        }
        if (plan.isActive != true) {
            throw IllegalStateException("Plano inativo: $planName")
        }
        val monthly = plan.pricePerUserMonthly
            ?: throw IllegalStateException("Plano sem price_per_user_monthly: $planName")
        val yearly = plan.pricePerUserYearly

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val trialEnds = now.plusDays(trialDays)
        val trialEndDate = trialEnds.toLocalDate()

        val sub = Subscription().apply {
            subscriptionId = UUID.randomUUID()
            this.tenantId = tenantId
            this.planName = planName
            status = SubscriptionStatus.TRIAL.code
            billingCycle = BillingCycle.MONTHLY.code
            trialEndsAt = trialEnds
            currentPeriodStart = LocalDate.now(ZoneOffset.UTC)
            currentPeriodEnd = trialEndDate
            pricePerUserMonthlySnapshot = monthly.setScale(2, java.math.RoundingMode.HALF_UP)
            pricePerUserYearlySnapshot = yearly?.setScale(2, java.math.RoundingMode.HALF_UP)
            currency = "BRL"
            createdAt = now
            updatedAt = now
        }
        return subscriptionRepository.save(sub)
    }

    /**
     * Assinatura **paga** desde o início (sem trial): status ACTIVE, sem [trialEndsAt].
     * Uso: cliente escolhe plano pago no cadastro ou após checkout (até integrar gateway).
     */
    @Transactional
    fun createActiveSubscriptionWithoutTrial(
        tenantId: UUID,
        planName: String,
        billingCycle: BillingCycle = BillingCycle.MONTHLY,
    ): Subscription {
        if (subscriptionRepository.findByTenantId(tenantId).isPresent) {
            throw IllegalStateException("Tenant já possui subscription")
        }
        val plan = planRepository.findById(planName).orElseThrow {
            IllegalArgumentException("Plano não encontrado: $planName")
        }
        if (plan.isActive != true) {
            throw IllegalStateException("Plano inativo: $planName")
        }
        val monthly = plan.pricePerUserMonthly
            ?: throw IllegalStateException("Plano sem price_per_user_monthly: $planName")
        val yearly = plan.pricePerUserYearly

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ym = YearMonth.from(LocalDate.now(ZoneOffset.UTC))
        val periodStart = ym.atDay(1)
        val periodEnd = ym.atEndOfMonth()

        val sub = Subscription().apply {
            subscriptionId = UUID.randomUUID()
            this.tenantId = tenantId
            this.planName = planName
            status = SubscriptionStatus.ACTIVE.code
            this.billingCycle = billingCycle.code
            trialEndsAt = null
            currentPeriodStart = periodStart
            currentPeriodEnd = periodEnd
            pricePerUserMonthlySnapshot = monthly.setScale(2, java.math.RoundingMode.HALF_UP)
            pricePerUserYearlySnapshot = yearly?.setScale(2, java.math.RoundingMode.HALF_UP)
            currency = "BRL"
            createdAt = now
            updatedAt = now
        }
        return subscriptionRepository.save(sub)
    }

    /**
     * TRIAL com trial_ends_at &lt; agora → EXPIRED (sem proration; cobrança mensal só após ACTIVE).
     */
    @Transactional
    fun expireTrials(): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val params = MapSqlParameterSource("now", now)
        return namedParameterJdbcTemplate.update(
            """
            UPDATE subscription
            SET status = '${SubscriptionStatus.EXPIRED.code}',
                updated_at = :now
            WHERE status = '${SubscriptionStatus.TRIAL.code}'
              AND trial_ends_at IS NOT NULL
              AND trial_ends_at < :now
            """.trimIndent(),
            params,
        )
    }

    /**
     * Quando o gateway confirmar pagamento (Stripe etc.), promove para ACTIVE.
     */
    @Transactional
    fun markActiveAfterPayment(subscriptionId: UUID, paymentProviderCustomerId: String?) {
        val sub = subscriptionRepository.findById(subscriptionId).orElseThrow {
            IllegalArgumentException("Subscription não encontrada")
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        sub.status = SubscriptionStatus.ACTIVE.code
        sub.trialEndsAt = null
        sub.paymentProviderCustomerId = paymentProviderCustomerId
        sub.updatedAt = now
        subscriptionRepository.save(sub)
    }
}
