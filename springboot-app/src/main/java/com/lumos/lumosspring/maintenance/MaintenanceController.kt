package com.lumos.lumosspring.maintenance

import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.maintenance.service.MaintenanceService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

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

}