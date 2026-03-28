package com.lumos.lumosspring.billing.service

import com.lumos.lumosspring.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Contagem de seats faturáveis por tenant.
 *
 * Regra: usuários com `status = true`, `support = false`, `deactivated_at IS NULL`.
 * Alinha com a query `countBillableActiveSeats` (mesma base usada para invoice).
 */
@Service
class BillingSeatService(
    private val userRepository: UserRepository,
) {
    fun countBillableSeats(tenantId: UUID): Long =
        userRepository.countBillableActiveSeats(tenantId) ?: 0L
}
