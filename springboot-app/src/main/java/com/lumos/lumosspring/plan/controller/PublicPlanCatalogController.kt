package com.lumos.lumosspring.plan.controller

import com.lumos.lumosspring.plan.repository.PlanRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class PlanPublicDto(
    val planName: String,
    val description: String?,
    val pricePerUserMonthly: BigDecimal?,
    val pricePerUserYearly: BigDecimal?,
)

/**
 * Catálogo público de planos (landing / signup). Rotas sob `public` já são permitidas no SecurityConfig.
 */
@RestController
@RequestMapping("/public/plans")
class PublicPlanCatalogController(
    private val planRepository: PlanRepository,
) {
    @GetMapping
    fun listActivePlans(): List<PlanPublicDto> =
        planRepository.findAllByIsActiveTrueOrderByPlanNameAsc().map { p ->
            PlanPublicDto(
                planName = p.planName,
                description = p.description,
                pricePerUserMonthly = p.pricePerUserMonthly,
                pricePerUserYearly = p.pricePerUserYearly,
            )
        }
}
