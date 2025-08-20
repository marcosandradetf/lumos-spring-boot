package com.lumos.lumosspring.maintenance

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.dto.maintenance.SendMaintenanceDTO
import com.lumos.lumosspring.dto.maintenance.MaintenanceStreetWithItems
import com.lumos.lumosspring.maintenance.service.MaintenanceService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Controller
@RequestMapping("/api")
class MaintenanceController(
    private val maintenanceService: MaintenanceService
) {

    @PostMapping("/maintenance/send-street")
    fun sendStreet(
        @RequestBody street: MaintenanceStreetWithItems,
    ): ResponseEntity<Any> {
        return maintenanceService.saveStreet(street)
    }

    @PostMapping("/maintenance/finish-maintenance")
    fun finishMaintenance(
        @RequestPart("signature") signature: MultipartFile?,
        @RequestPart("maintenance") maintenance: SendMaintenanceDTO?
    ): ResponseEntity<Any> {
        return maintenanceService.finishMaintenance(maintenance, signature)
    }

    @GetMapping("/maintenance/get-finished")
    fun getGroupedMaintenances(): ResponseEntity<List<Map<String, JsonNode>>> =
        ResponseEntity.ok(maintenanceService.getGroupedMaintenances())

    @PostMapping("/maintenance/generate-report/{type}/{maintenanceId}")
    fun generateReport(
        @PathVariable type: String,
        @PathVariable maintenanceId: UUID,
    ): ResponseEntity<ByteArray> {
        return when (type.lowercase()) {
            "conventional" -> maintenanceService.conventionalReport(maintenanceId)
            "led" -> maintenanceService.ledReport(maintenanceId) // Supondo que exista outro método
            else -> ResponseEntity.badRequest().body(ByteArray(0))
        }
    }

    @PostMapping("/maintenance/archive-or-delete")
    fun archiveOrDelete(@RequestBody payload: Map<String, Any>): ResponseEntity<Any> {
        return maintenanceService.archiveOrDelete(payload)
    }


}