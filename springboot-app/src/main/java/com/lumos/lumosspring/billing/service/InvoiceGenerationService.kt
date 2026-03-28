package com.lumos.lumosspring.billing.service

import com.lumos.lumosspring.billing.domain.BillingCycle
import com.lumos.lumosspring.billing.domain.InvoiceStatus
import com.lumos.lumosspring.billing.domain.SubscriptionStatus
import com.lumos.lumosspring.billing.model.Invoice
import com.lumos.lumosspring.billing.repository.InvoiceRepository
import com.lumos.lumosspring.billing.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/**
 * Fechamento mensal por mês civil anterior (sem proration por dia).
 * Idempotência: UNIQUE (subscription_id, period_start) no banco.
 *
 * Apenas [SubscriptionStatus.ACTIVE] e [SubscriptionStatus.PAST_DUE] com ciclo MONTHLY geram fatura mensal.
 * TRIAL não gera fatura (trial gratuito).
 */
@Service
class InvoiceGenerationService(
    private val subscriptionRepository: SubscriptionRepository,
    private val invoiceRepository: InvoiceRepository,
    private val billingSeatService: BillingSeatService,
) {

    @Transactional
    fun generateMonthlyInvoicesForPreviousCalendarMonth(): Int {
        val period = YearMonth.now(ZoneOffset.UTC).minusMonths(1)
        val periodStart = period.atDay(1)
        val periodEnd = period.atEndOfMonth()
        var created = 0

        val billableStatuses = setOf(
            SubscriptionStatus.ACTIVE.code,
            SubscriptionStatus.PAST_DUE.code,
        )

        for (sub in subscriptionRepository.findAll()) {
            if (sub.billingCycle != BillingCycle.MONTHLY.code) continue
            if (sub.status !in billableStatuses) continue
            val sid = sub.subscriptionId ?: continue
            val tenantId = sub.tenantId ?: continue

            if (invoiceRepository.existsBySubscriptionIdAndPeriodStart(sid, periodStart)) {
                continue
            }

            val seats = billingSeatService.countBillableSeats(tenantId)
            val unit = sub.pricePerUserMonthlySnapshot ?: BigDecimal.ZERO
            val amount = unit.multiply(BigDecimal.valueOf(seats)).setScale(2, RoundingMode.HALF_UP)

            val invoice = Invoice().apply {
                invoiceId = UUID.randomUUID()
                subscriptionId = sid
                this.tenantId = tenantId
                this.periodStart = periodStart
                this.periodEnd = periodEnd
                billingCycle = BillingCycle.MONTHLY.code
                seatCount = seats.toInt()
                unitPriceSnapshot = unit.setScale(2, RoundingMode.HALF_UP)
                amountTotal = amount
                currency = sub.currency ?: "BRL"
                status = InvoiceStatus.OPEN.code
                issuedAt = OffsetDateTime.now(ZoneOffset.UTC)
            }
            invoiceRepository.save(invoice)
            created++
        }
        return created
    }
}
