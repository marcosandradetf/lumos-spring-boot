package com.thryon.lumos.application

import com.thryon.lumos.infrastructure.repository.MaintenanceRepository
import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject


@ApplicationScoped
class MaintenanceService @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository
) {
    fun debitStockForMaintenance(items: List<MaintenanceRepository.MaintenanceStreetItemDTO>) {
        maintenanceRepository.debitStock(items)
    }

    fun getGroupedMaintenances(): List<JsonObject> {
        return maintenanceRepository.getGroupedMaintenances()
    }


}