package com.lumos.lumosspring.metrics

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/metrics")
class MetricsController(
    private val metricsService: MetricsService
) {
    @GetMapping("/get-metrics")
    fun getMetrics() = metricsService.getDashboardMetrics()
}