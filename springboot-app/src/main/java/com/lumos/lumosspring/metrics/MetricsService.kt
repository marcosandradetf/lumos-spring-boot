package com.lumos.lumosspring.metrics

import com.lumos.lumosspring.util.Utils
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MetricsService(
    private val metricsRepository: MetricsRepository
) {
    fun getDashboardMetrics(): ResponseEntity<Any> {
        return ResponseEntity.ok().body(metricsRepository.findDashboardMetrics(Utils.getCurrentTenantId()))
    }
}