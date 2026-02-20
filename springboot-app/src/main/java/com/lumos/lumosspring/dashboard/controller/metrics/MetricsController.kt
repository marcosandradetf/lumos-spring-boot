package com.lumos.lumosspring.dashboard.controller.metrics

import com.lumos.lumosspring.dashboard.service.metrics.MetricsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard/metrics")
class MetricsController(
    private val metricsService: MetricsService
) {
    @GetMapping("/get-metrics")
    fun getMetrics() = metricsService.getDashboardMetrics()
}