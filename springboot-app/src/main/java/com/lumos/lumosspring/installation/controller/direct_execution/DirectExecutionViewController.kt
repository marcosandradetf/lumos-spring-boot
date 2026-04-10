package com.lumos.lumosspring.installation.controller.direct_execution

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.installation.dto.direct_execution.DirectExecutionDTOResponse
import com.lumos.lumosspring.installation.service.direct_execution.DirectExecutionViewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api")
class DirectExecutionViewController(
    private val viewService: DirectExecutionViewService,
) {
    @GetMapping("/mobile/execution/get-direct-executions")
    fun getDirectExecutions(@RequestParam uuid: String?): ResponseEntity<List<DirectExecutionDTOResponse>> =
        viewService.getDirectExecutions(uuid)

    @GetMapping("/mobile/v2/execution/get-direct-executions")
    fun getDirectExecutionsV2(@RequestParam teamId: Long, @RequestParam status: String): ResponseEntity<List<DirectExecutionDTOResponse>> =
        viewService.getDirectExecutionsV2(teamId, status)


    @GetMapping("/execution/get-finished")
    fun getGroupedInstallations(
        @RequestParam startDate: Instant,
        @RequestParam endDate: Instant
    ): ResponseEntity<List<Map<String, JsonNode>>> =
        ResponseEntity.ok(
            viewService.getGroupedInstallations(
                startDate = startDate.atOffset(java.time.ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.DAYS),
                endDate = endDate.atOffset(java.time.ZoneOffset.UTC)
                    .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)
            )
        )

    @GetMapping("/execution/contract/{id}/items-for-link")
    fun getContractItemsForLink(
        @PathVariable id: Long
    ): ResponseEntity<Any> {
        return viewService.getContractItemsForLink(id)
    }

    @GetMapping("/execution/get-installations-waiting-validation")
    fun getInstallationsWaitingValidation(): ResponseEntity<Any> {
        return viewService.getInstallationsWaitingValidation()
    }

    @GetMapping("/execution/{id}/waiting-validation")
    fun getExecutionWaitingValidation(
        @PathVariable id: Long
    ): ResponseEntity<Any> {
        return viewService.getExecutionWaitingValidation(id)
    }

}
