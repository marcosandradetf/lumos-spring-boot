package com.lumos.lumosspring.billing.schedule

import com.lumos.lumosspring.billing.service.InvoiceGenerationService
import com.lumos.lumosspring.billing.service.SubscriptionLifecycleService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Jobs de billing: diário (trial) e mensal (fatura do mês civil anterior).
 */
@Component
@ConditionalOnProperty(name = ["lumos.billing.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class BillingScheduledTasks(
    private val subscriptionLifecycleService: SubscriptionLifecycleService,
    private val invoiceGenerationService: InvoiceGenerationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${lumos.billing.cron.daily:0 0 6 * * *}")
    fun expireTrialsDaily() {
        val n = subscriptionLifecycleService.expireTrials()
        if (n > 0) {
            log.info("Billing: trials expirados (linhas atualizadas): {}", n)
        }
    }

    /** Dia 1 de cada mês, fecha o mês anterior (UTC). */
    @Scheduled(cron = "\${lumos.billing.cron.monthly:0 0 7 1 * *}")
    fun monthlyInvoices() {
        val n = invoiceGenerationService.generateMonthlyInvoicesForPreviousCalendarMonth()
        log.info("Billing: faturas mensais geradas: {}", n)
    }
}
