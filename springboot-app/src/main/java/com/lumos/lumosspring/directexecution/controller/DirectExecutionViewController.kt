package com.lumos.lumosspring.directexecution.controller

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.directexecution.dto.DirectExecutionDTOResponse
import com.lumos.lumosspring.directexecution.service.DirectExecutionViewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class DirectExecutionViewController(
    private val viewService: DirectExecutionViewService,
) {
    @GetMapping("/mobile/execution/get-direct-executions")
    fun getDirectExecutions(@RequestParam uuid: String?): ResponseEntity<List<DirectExecutionDTOResponse>> =
        viewService.getDirectExecutions(uuid)


    @GetMapping("/execution/get-finished")
    fun getGroupedInstallations(): ResponseEntity<List<Map<String, JsonNode>>> =
        ResponseEntity.ok(viewService.getGroupedInstallations())

}