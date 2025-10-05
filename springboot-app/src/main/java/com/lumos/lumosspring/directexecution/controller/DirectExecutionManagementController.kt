package com.lumos.lumosspring.directexecution.controller

import com.lumos.lumosspring.directexecution.dto.DirectExecutionDTO
import com.lumos.lumosspring.directexecution.service.DirectExecutionManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class DirectExecutionManagementController(
    private val managementService: DirectExecutionManagementService,
) {
    @PostMapping("/execution/delegate-direct-execution")
    fun delegateDirectExecution(@RequestBody execution: DirectExecutionDTO): ResponseEntity<Any> =
        managementService.delegateDirectExecution(execution)

    @PostMapping("/execution/cancel-step")
    fun cancelStep(@RequestBody payload: Map<String, Any>): ResponseEntity<Any> {
        return managementService.cancelStep(payload)
    }

    @PostMapping("/execution/archive-or-delete")
    fun archiveOrDelete(@RequestBody payload: Map<String, Any>): ResponseEntity<Any> {
        return managementService.archiveOrDelete(payload)
    }

}