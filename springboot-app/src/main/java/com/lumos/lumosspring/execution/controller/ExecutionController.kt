package com.lumos.lumosspring.execution.controller

import com.lumos.lumosspring.execution.dto.ReserveDTO
import com.lumos.lumosspring.execution.service.ExecutionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ExecutionController(
    private val executionService: ExecutionService,
) {

    @PostMapping("/execution/reserve")
    fun reserve(@RequestBody reserveDto : List<ReserveDTO>): ResponseEntity<Any> {
        return executionService.reserve(reserveDto)
    }

    @GetMapping("/execution/get-available-stock")
    fun getStockAvailable(
        @RequestParam preMeasurementId: Long,
        @RequestParam teamId: Long
    ) : ResponseEntity<Any> {
        return executionService.getStockAvailable(preMeasurementId, teamId)
    }
}