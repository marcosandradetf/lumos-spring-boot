package com.lumos.lumosspring.billing.domain

import java.time.OffsetDateTime

/**
 * Estados da assinatura (estilo Stripe: trial → pago ou expira).
 */
enum class SubscriptionStatus(val code: String) {
    TRIAL("TRIAL"),
    ACTIVE("ACTIVE"),
    PAST_DUE("PAST_DUE"),
    CANCELED("CANCELED"),
    EXPIRED("EXPIRED");

    companion object {
        fun fromCode(value: String): SubscriptionStatus =
            entries.firstOrNull { it.code == value }
                ?: throw IllegalArgumentException("Unknown subscription status: $value")

        fun fromCodeOrNull(value: String?): SubscriptionStatus? =
            value?.let { v -> entries.firstOrNull { it.code == v } }
    }

    /**
     * Se o usuário pode usar o app (API autenticada) neste instante.
     * [trialEndsAt] obrigatório quando status é TRIAL.
     */
    fun allowsAppAccess(now: OffsetDateTime, trialEndsAt: OffsetDateTime?): Boolean = when (this) {
        CANCELED, EXPIRED -> false
        TRIAL -> trialEndsAt != null && trialEndsAt.isAfter(now)
        ACTIVE, PAST_DUE -> true
    }

    /** Motivo curto para logs / UI quando o status já é terminal. */
    fun accessDeniedReason(): String? = when (this) {
        EXPIRED -> "Trial ou assinatura encerrada."
        CANCELED -> "Assinatura cancelada."
        else -> null
    }
}

enum class BillingCycle(val code: String) {
    MONTHLY("MONTHLY"),
    YEARLY("YEARLY");

    companion object {
        fun fromCode(value: String): BillingCycle =
            entries.firstOrNull { it.code == value }
                ?: throw IllegalArgumentException("Unknown billing cycle: $value")
    }
}

enum class InvoiceStatus(val code: String) {
    DRAFT("DRAFT"),
    OPEN("OPEN"),
    PAID("PAID"),
    VOID("VOID"),
    UNCOLLECTIBLE("UNCOLLECTIBLE");

    companion object {
        fun fromCode(value: String): InvoiceStatus =
            entries.firstOrNull { it.code == value }
                ?: throw IllegalArgumentException("Unknown invoice status: $value")
    }
}
