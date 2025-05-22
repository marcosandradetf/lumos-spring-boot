package com.lumos.lumosspring.execution.controller

import com.lumos.lumosspring.execution.dto.DelegateDTO
import com.lumos.lumosspring.execution.dto.ReserveForStreetsDTO
import com.lumos.lumosspring.execution.service.ExecutionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ExecutionController(
    private val executionService: ExecutionService,
) {

    @PostMapping("/execution/delegate")
    fun delegate(@RequestBody delegateDTO: DelegateDTO): ResponseEntity<Any> = executionService.delegate(delegateDTO)

    @GetMapping("execution/get-reservations/{userUUID}")
    fun getPendingReservesForStockist(@PathVariable userUUID: String): ResponseEntity<Any> =
        executionService.getPendingReservesForStockist(userUUID)

//    @PostMapping("/execution/reserve")
//    fun reserve(@RequestBody reserveDto : ReserveForStreetsDTO): ResponseEntity<Any> {
//        return executionService.reserve(reserveDto)
//    }
//
//    @GetMapping("/execution/get-available-stock")
//    fun getStockAvailable(
//        @RequestParam preMeasurementId: Long,
//        @RequestParam teamId: Long
//    ) : ResponseEntity<Any> {
//        return executionService.getStockAvailable(preMeasurementId, teamId)
//    }
}