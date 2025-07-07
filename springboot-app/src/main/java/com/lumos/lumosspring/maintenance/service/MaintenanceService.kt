package com.lumos.lumosspring.maintenance.service

import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MaintenanceService(
    private val maintenanceQueryRepository: MaintenanceQueryRepository
) {

    fun getMaterialsForMaintenance(): ResponseEntity<List<MaintenanceQueryRepository.TypesMaterialDto>> {
        return ResponseEntity.ok(maintenanceQueryRepository.getMaterialsForMaintenance())
    }

}