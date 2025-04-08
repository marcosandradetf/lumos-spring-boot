package com.lumos.lumosspring.execution.controller

import com.lumos.lumosspring.execution.dto.ReserveDTO
import com.lumos.lumosspring.execution.service.ExecutionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ExecutionController(
    private val executionService: ExecutionService,
) {

    @GetMapping("/execution/reserve")
    fun reserve(@RequestBody reserveDto : List<ReserveDTO>): ResponseEntity<Any> {
        return executionService.reserve(reserveDto)
    }
}