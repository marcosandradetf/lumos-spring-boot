package com.lumos.lumosspring.maintenance

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.maintenance.service.MaintenanceService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Indexed
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Indexed
@Controller
@RequestMapping("/api")
class MaintenanceController(
    private val maintenanceService: MaintenanceService
) {

    @PostMapping("/maintenance/send-street")
    fun sendStreet(
        @RequestBody street: MaintenanceQueryRepository.MaintenanceStreetWithItems,
    ): ResponseEntity<Any> {
        return maintenanceService.saveStreet(street)
    }

    @PostMapping("/maintenance/finish-maintenance")
    fun finishMaintenance(
        @RequestPart("signature") signature: MultipartFile?,
        @RequestPart("maintenance") maintenance: MaintenanceQueryRepository.MaintenanceDTO?
    ): ResponseEntity<Any> {
        return maintenanceService.finishMaintenance(maintenance, signature)
    }

    @GetMapping("/maintenance/get-finished")
    fun getGroupedMaintenances(): ResponseEntity<List<Map<String, JsonNode>>> =
        ResponseEntity.ok(maintenanceService.getGroupedMaintenances())


}