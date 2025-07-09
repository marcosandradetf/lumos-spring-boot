package com.lumos.lumosspring.maintenance

import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.maintenance.service.MaintenanceService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/api")
class MaintenanceController(
    private val maintenanceService: MaintenanceService
) {

    @GetMapping("/mobile/maintenance/get-stock")
    fun getStock(
        @RequestParam(value = "uuid", required = true) uuid: String,
    ): ResponseEntity<List<MaintenanceQueryRepository.TypesMaterialDto>> {
        return maintenanceService.getMaterialsForMaintenance()
    }

}