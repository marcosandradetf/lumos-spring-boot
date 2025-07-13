package com.lumos.lumosspring.maintenance.service

import com.lumos.lumosspring.maintenance.entities.Maintenance
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreet
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreetItem
import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceStreetItemRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceStreetRepository
import com.lumos.lumosspring.stock.repository.StockQueryRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class MaintenanceService(
    private val maintenanceRepository: MaintenanceRepository,
    private val maintenanceStreetRepository: MaintenanceStreetRepository,
    private val maintenanceStreetItemRepository: MaintenanceStreetItemRepository,
    private val maintenanceQueryRepository: MaintenanceQueryRepository
) {
    fun finishMaintenance(
        maintenance: MaintenanceQueryRepository.MaintenanceDTO
    ): ResponseEntity<Any> {
        var maintenanceUuid: UUID
        var dateOfVisit: Instant

        try {
            maintenanceUuid = UUID.fromString(maintenance.maintenanceId)
            dateOfVisit = Instant.parse(maintenance.dateOfVisit)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException(ex.message)
        }

        val newMaintenance = Maintenance(
            maintenanceId = maintenanceUuid,
            contractId = maintenance.contractId,
            pendingPoints = maintenance.pendingPoints,
            quantityPendingPoints = maintenance.quantityPendingPoints,
            dateOfVisit = dateOfVisit,
            type = maintenance.type,
            status = "FINISHED",
            isNewEntry = false,
        )

        maintenanceRepository.save(newMaintenance)

        return ResponseEntity.noContent().build()
    }

    @Transactional
    fun saveStreet(
        street: MaintenanceQueryRepository.MaintenanceStreetWithItems
    ): ResponseEntity<Any> {
        var maintenanceUuid: UUID
        var maintenanceStreetUuid: UUID

        try {
            maintenanceUuid = UUID.fromString(street.street.maintenanceId)
            maintenanceStreetUuid = UUID.fromString(street.street.maintenanceStreetId)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException(ex.message)
        }

        var exists = maintenanceRepository.existsById(maintenanceUuid)
        if (!exists) {
            val newMaintenance = Maintenance(
                maintenanceId = maintenanceUuid,
                contractId = null,
                pendingPoints = false,
                quantityPendingPoints = null,
                dateOfVisit = Instant.now(),
                type = "",
                status = "DRAFT",
            )

            maintenanceRepository.save(newMaintenance)
        }

        exists = maintenanceStreetRepository.existsById(maintenanceStreetUuid)
        if (exists) {
            return ResponseEntity.noContent().build()
        }

        val newStreet = MaintenanceStreet(
            maintenanceStreetId = maintenanceStreetUuid,
            maintenanceId = maintenanceUuid,
            address = street.street.address,
            latitude = street.street.latitude,
            longitude = street.street.longitude,
            comment = street.street.comment,
            lastPower = street.street.lastPower,
            lastSupply = street.street.lastSupply,
            currentSupply = street.street.currentSupply,
            reason = street.street.reason,
        )

        maintenanceStreetRepository.save(newStreet)

        val items = street.items.map {
            MaintenanceStreetItem(
                maintenanceId = maintenanceUuid,
                maintenanceStreetId = maintenanceStreetUuid,
                materialStockId = it.materialStockId,
                quantityExecuted = it.quantityExecuted,
            )
        }

        maintenanceStreetItemRepository.saveAll(items)

        maintenanceQueryRepository.debitStock(street.items)

        return ResponseEntity.noContent().build()
    }

}