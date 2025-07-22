package com.lumos.lumosspring.maintenance.service

import com.lumos.lumosspring.fileserver.service.MinioService
import com.lumos.lumosspring.maintenance.entities.Maintenance
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreet
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreetItem
import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceStreetItemRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceStreetRepository
import com.lumos.lumosspring.stock.repository.TeamQueryRepository
import com.lumos.lumosspring.util.Utils.getCurrentUserId
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.*

@Service
class MaintenanceService(
    private val maintenanceRepository: MaintenanceRepository,
    private val maintenanceStreetRepository: MaintenanceStreetRepository,
    private val maintenanceStreetItemRepository: MaintenanceStreetItemRepository,
    private val maintenanceQueryRepository: MaintenanceQueryRepository,
    private val teamQueryRepository: TeamQueryRepository,
    private val minioService: MinioService,
) {
    @Transactional
    fun finishMaintenance(
        maintenance: MaintenanceQueryRepository.MaintenanceDTO?,
        signature: MultipartFile?
    ): ResponseEntity<Any> {
        var maintenanceUuid: UUID
        var dateOfVisit: Instant
        var signDate: Instant?
        var userId: UUID

        if (maintenance == null) {
            return ResponseEntity.badRequest().body("Execution DTO está vazio.")
        }

        try {
            maintenanceUuid = UUID.fromString(maintenance.maintenanceId)
            dateOfVisit = Instant.parse(maintenance.dateOfVisit)
            signDate = maintenance.signDate?.let {Instant.parse(it)}
            userId = getCurrentUserId()
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException(ex.message)
        }

        val teamId = teamQueryRepository.getTeamIdByUserId(userId) ?: throw IllegalStateException("Maintenance Service - Equipe não cadastrada para o usuário atual")

        val fileUri = signature?.let {
            val folder = "photos/maintenance/${maintenance.responsible?.replace("\\s+".toRegex(), "_")}"
            minioService.uploadFile(it, "scl-construtora", folder, "execution")
        }

        val newMaintenance = Maintenance(
            maintenanceId = maintenanceUuid,
            contractId = maintenance.contractId,
            pendingPoints = maintenance.pendingPoints,
            quantityPendingPoints = maintenance.quantityPendingPoints,
            dateOfVisit = dateOfVisit,
            type = maintenance.type,
            status = "FINISHED",
            teamId = teamId,

            signatureUri = fileUri,
            responsible = maintenance.responsible,
            signDate = signDate,

            isNewEntry = false,
        )

        maintenanceRepository.save(newMaintenance)

        return ResponseEntity.noContent().build()
    }

    @Transactional
    fun saveStreet(
        street: MaintenanceQueryRepository.MaintenanceStreetWithItems,
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
                teamId = null,
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