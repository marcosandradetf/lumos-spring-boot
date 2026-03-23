package com.lumos.lumosspring.installation.controller.direct_execution

import com.lumos.lumosspring.installation.service.direct_execution.DirectExecutionReportService
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

    @PostMapping("/execution/generate-report/{type}/{executionId}/{executionType}")
    fun generateReport(
        @PathVariable type: String,
        @PathVariable executionId: Long,
        @PathVariable executionType: String
    ): ResponseEntity<ByteArray> {
        if (type == "data") {
            return reportService.generateDataReport(executionId, executionType)
        } else if (type == "photos") {
            return reportService.generatePhotoReport(executionId, executionType)
        } else {
            return ResponseEntity.badRequest().body(ByteArray(0))
        }

    }

}