package com.lumos.lumosspring.maintenance

import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.maintenance.service.MaintenanceService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api")
class MaintenanceController(
    private val maintenanceService: MaintenanceService
) {

    @GetMapping("/maintenance/get-materials")
    fun getMaterialsForMaintenance(): ResponseEntity<List<MaintenanceQueryRepository.TypesMaterialDto>> {
        return maintenanceService.getMaterialsForMaintenance()
    }

}