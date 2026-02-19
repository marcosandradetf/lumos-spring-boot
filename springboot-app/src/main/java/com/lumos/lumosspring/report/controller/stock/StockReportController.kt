package com.lumos.lumosspring.report.controller.stock

import com.lumos.lumosspring.report.service.installation.ReportService
import com.lumos.lumosspring.report.service.stock.MaterialReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/report")
class StockReportController(
    private val reportService: MaterialReportService,
) {

    data class FiltersRequest(
        val contractId: Long?,
        val startDate: Instant,
        val endDate: Instant,
    )
    @PostMapping("/stock/generate-material-reservation-report")
    fun generateReport(@RequestBody filters: FiltersRequest): ResponseEntity<Any> {
        return reportService.generateHtml(filters)
    }



}