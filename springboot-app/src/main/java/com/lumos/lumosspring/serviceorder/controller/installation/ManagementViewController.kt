package com.lumos.lumosspring.serviceorder.controller.installation

import com.lumos.lumosspring.serviceorder.service.installation.ManagementViewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/service-order")
class ManagementViewController(
    private val service: ManagementViewService
) {
    @GetMapping("/get-executions/{status}")
    fun getExecutions(
        @PathVariable status: String,
        @RequestParam(required = false) contractId: Long?
    ): ResponseEntity<Any> {
        return service.getExecutions(status, contractId)
    }

    @GetMapping("/get-reservations")
    fun getPendingReservesForStockist(): ResponseEntity<Any> {
        return service.getPendingReservesForStockist()
    }


}