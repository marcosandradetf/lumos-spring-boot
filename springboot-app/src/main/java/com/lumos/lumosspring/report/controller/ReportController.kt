package com.lumos.lumosspring.report.controller

import com.lumos.lumosspring.report.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/report")
class ReportController(
    private val reportService: ReportService
) {

    @PostMapping("/pdf/generate/{title}")
    fun generatePdf(@RequestBody htmlRequest: String, @PathVariable title: String): ResponseEntity<ByteArray?> {
        return reportService.generatePdf(htmlRequest, title)
    }
}