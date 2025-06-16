package com.lumos.lumosspring.execution.controller

import com.lumos.lumosspring.execution.dto.DelegateDTO
import com.lumos.lumosspring.execution.dto.DirectExecutionDTO
import com.lumos.lumosspring.execution.dto.ExecutionDTO
import com.lumos.lumosspring.execution.dto.ReserveDTOCreate
import com.lumos.lumosspring.execution.dto.SendExecutionDto
import com.lumos.lumosspring.execution.service.ExecutionService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api")
class ExecutionController(
    private val executionService: ExecutionService,
) {

    @PostMapping("/execution/delegate")
    fun delegate(@RequestBody delegateDTO: DelegateDTO): ResponseEntity<Any> = executionService.delegate(delegateDTO)

    @PostMapping("/execution/delegate-direct-execution")
    fun delegateDirectExecution(@RequestBody execution: DirectExecutionDTO): ResponseEntity<Any> = executionService.delegateDirectExecution(execution)

    @GetMapping("execution/get-reservations/{userUUID}")
    fun getPendingReservesForStockist(@PathVariable userUUID: String): ResponseEntity<Any> =
        executionService.getPendingReservesForStockist(userUUID)

    @GetMapping("execution/get-stock-materials/{linking}/{type}/{truckDepositName}")
    fun getStockMaterialForLinking(
        @PathVariable linking: String,
        @PathVariable type: String,
        @PathVariable truckDepositName: String
    ): ResponseEntity<Any> =
        executionService.getStockMaterialForLinking(
            linking,
            type,
            truckDepositName
        )

    @PostMapping("/execution/reserve-materials-for-execution/{userUUID}")
    fun reserveMaterialsForExecution(
        @RequestBody reserveDTOCreate: ReserveDTOCreate,
        @PathVariable userUUID: String
    ): ResponseEntity<Any> = executionService.reserveMaterialsForExecution(reserveDTOCreate, userUUID)

    @PostMapping("/execution/get-reservations-by-status-and-stockist")
    fun getReservationsByStatusAndStockist(
        @RequestParam uuid: String,
        @RequestParam status: String,
    ) = executionService.getReservationsByStatusAndStockist(uuid, status)

    @PostMapping(
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        value = ["/mobile/execution/upload"]
    )
    fun uploadData(
        @RequestPart("photo") photo: MultipartFile,
        @RequestPart("execution") execution: SendExecutionDto?
    ): ResponseEntity<Any> = executionService.uploadData(photo, execution)


    @GetMapping("/mobile/execution/get-executions")
    fun getExecutions(@RequestParam uuid: String?): ResponseEntity<MutableList<ExecutionDTO>> = executionService.getExecutionsByStreets(uuid)

}