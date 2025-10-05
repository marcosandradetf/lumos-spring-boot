package com.lumos.lumosspring.directexecution.controller

import com.lumos.lumosspring.directexecution.service.DirectExecutionReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class DirectExecutionReportController(
    private val reportService: DirectExecutionReportService
) {

    @PostMapping("/execution/generate-report/{type}/{executionId}")
    fun generateReport(@PathVariable type: String, @PathVariable executionId: Long): ResponseEntity<ByteArray> {
        if (type == "data") {
            return reportService.generateDataReport(executionId)
        } else if (type == "photos") {
            return reportService.generatePhotoReport(executionId)
        } else {
            return ResponseEntity.badRequest().body(ByteArray(0))
        }

    }

}