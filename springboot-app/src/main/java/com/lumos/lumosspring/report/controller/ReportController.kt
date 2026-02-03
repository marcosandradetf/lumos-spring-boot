package com.lumos.lumosspring.report.controller

import com.lumos.lumosspring.report.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/report")
class ReportController(
    private val reportService: ReportService
) {

    @PostMapping("/pdf/generate/{title}")
    fun generatePdf(@RequestBody htmlRequest: String, @PathVariable title: String): ResponseEntity<ByteArray?> {
        return reportService.generatePdf(htmlRequest, title)
    }

    data class FiltersRequest(
        val contractId: Long,
        val type: String, // MAINTENANCE: CONVENTIONAL | LED || INSTALLATION: LED | PHOTO
        val startDate: Instant,
        val endDate: Instant,
        val viewMode: String, // LIST | GROUPED
        val scope: String, // MAINTENANCE | INSTALLATION
        val executionId: String? = null
    )

    @PostMapping("/execution/generate-report")
    fun generateReport(@RequestBody filtersRequest: FiltersRequest): ResponseEntity<Any> {
        return reportService.generateExecutionReport(filtersRequest)

    }

    @GetMapping("/execution/get-contracts")
    fun getContracts(): ResponseEntity<Any> {
        return reportService.getContracts()
    }


}