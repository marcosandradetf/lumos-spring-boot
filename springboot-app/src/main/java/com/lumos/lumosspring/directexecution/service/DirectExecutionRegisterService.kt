package com.lumos.lumosspring.directexecution.service

import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.directexecution.dto.InstallationRequest
import com.lumos.lumosspring.directexecution.dto.InstallationStreetRequest
import com.lumos.lumosspring.directexecution.model.DirectExecutionExecutor
import com.lumos.lumosspring.directexecution.model.DirectExecutionStreet
import com.lumos.lumosspring.directexecution.model.DirectExecutionStreetItem
import com.lumos.lumosspring.directexecution.repository.*
import com.lumos.lumosspring.minio.service.MinioService
import com.lumos.lumosspring.stock.materialsku.repository.MaterialContractReferenceItemRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil
import com.lumos.lumosspring.util.ReservationStatus
import com.lumos.lumosspring.util.Utils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.*


@Service
class DirectExecutionRegisterService(
    private val minioService: MinioService,
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val directExecutionRepositoryStreet: DirectExecutionRepositoryStreet,
    private val directExecutionRepositoryStreetItem: DirectExecutionRepositoryStreetItem,
    private val directExecutionExecutorRepository: DirectExecutionExecutorRepository,
    private val directExecutionRepository: DirectExecutionRepository,
    private val directExecutionRepositoryItem: DirectExecutionRepositoryItem,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val materialContractReferenceItemRepository: MaterialContractReferenceItemRepository,
) {

    @Transactional
    fun saveStreetInstallation(photo: MultipartFile, installationReq: InstallationStreetRequest?): ResponseEntity<Any> {
        if (installationReq == null) {
            return ResponseEntity.badRequest().body("payload vazio.")
        }

        val exists = JdbcUtil.getSingleRow(
            namedJdbc,
            """
                SELECT 1 as result
                FROM direct_execution de
                JOIN direct_execution_street des on des.direct_execution_id = de.direct_execution_id
                WHERE des.device_street_id = :deviceStreetId AND des.device_id = :deviceId and de.direct_execution_status = 'FINISHED'
            """.trimIndent(),
            mapOf("deviceStreetId" to installationReq.deviceStreetId, "deviceId" to installationReq.deviceId)
        )?.get("result") != null

        if (exists) {
            return ResponseEntity.status(409).body("Rua já enviada antes ou instalação já finalizada!")
        }

        var installationStreet = DirectExecutionStreet(
            lastPower = installationReq.lastPower,
            address = installationReq.address,
            latitude = installationReq.latitude,
            longitude = installationReq.longitude,
            deviceStreetId = installationReq.deviceStreetId,
            deviceId = installationReq.deviceId,
            finishedAt = installationReq.finishAt,
            directExecutionId = installationReq.directExecutionId,
            currentSupply = installationReq.currentSupply
        )

        val folder = "photos/${installationReq.description.replace("\\s+".toRegex(), "_")}"
        val fileUri = minioService.uploadFile(photo, Utils.getCurrentBucket(), folder, "installation")
        installationStreet.executionPhotoUri = fileUri

        try {
            installationStreet = directExecutionRepositoryStreet.save(installationStreet)
        } catch (ex: DataIntegrityViolationException) {
            val rootMessage = ex.mostSpecificCause.message ?: ""
            if (rootMessage.contains("UNIQUE_SEND_STREET", ignoreCase = true)) {
                return ResponseEntity.ok().build()
            }
            throw ex
        }

        for (m in installationReq.materials) {
            val balance = namedJdbc.queryForObject(
                """
                    select stock_quantity
                    from material_stock
                    where material_id_stock = :materialStockId
                    limit 1
                """.trimIndent(),
                mapOf("materialStockId" to m.truckMaterialStockId),
                BigDecimal::class.java
            )

            if (balance == null || balance < m.quantityExecuted) {
                throw Utils.BusinessException("Sem estoque para o material: " + m.truckMaterialStockId + " - " + m.materialName)
            }

            namedJdbc.update(
                """
                    UPDATE material_stock
                    SET stock_quantity = stock_quantity - :quantityCompleted,
                        stock_available = stock_available - :quantityCompleted
                    WHERE material_id_stock = :materialStockId
                """.trimIndent(),
                mapOf(
                    "quantityCompleted" to m.quantityExecuted,
                    "materialStockId" to m.truckMaterialStockId,
                )
            )

            namedJdbc.update(
                """
                    UPDATE material_reservation
                    SET quantity_completed = quantity_completed + :quantityCompleted
                    WHERE material_id_reservation = :reserveId
                """.trimIndent(),
                mapOf(
                    "quantityCompleted" to m.quantityExecuted,
                    "reserveId" to m.reserveId,
                )
            )

            val item = DirectExecutionStreetItem(
                executedQuantity = m.quantityExecuted,
                materialStockId = m.truckMaterialStockId,
                contractItemId = m.contractItemId,
                directExecutionStreetId = installationStreet.directExecutionStreetId
                    ?: throw IllegalStateException("directExecutionStreetId not setted")
            )

            directExecutionRepositoryStreetItem.save(item)

        }

        val materialsPair = installationReq.materials.map { Pair(it.truckMaterialStockId, it.quantityExecuted) }
        val servicesPair = materialContractReferenceItemRepository.findByContractReferenceItemId(
            materialsPair.map { it.first },
            installationReq.directExecutionId
        )

        servicesPair.forEach { s ->
            val contractItemId = s.first
            val materialId = s.second
            val quantityExecuted = materialsPair.find { it.first == materialId }?.second

            quantityExecuted?.let {
                directExecutionRepositoryStreetItem.save(
                    DirectExecutionStreetItem(
                        executedQuantity = it,
                        materialStockId = null,
                        contractItemId = contractItemId,
                        directExecutionStreetId = installationStreet.directExecutionStreetId
                            ?: throw IllegalStateException("directExecutionStreetId not set")
                    )
                )
            }
        }

        return ResponseEntity.ok().build()
    }

    @Transactional
    fun finishDirectExecution(directExecutionId: Long, operationalUsers: List<UUID>? = null): ResponseEntity<Any> {
        val installation = directExecutionRepository.getInstallation(directExecutionId)
        val status = installation?.status

        if (status == ExecutionStatus.FINISHED) {
            return ResponseEntity.noContent().build()
        }

        operationalUsers?.let { users ->
            directExecutionExecutorRepository.saveAll(
                users.map { userId ->
                    DirectExecutionExecutor(
                        directExecutionId = directExecutionId,
                        userId = userId,
                        isNewEntry = true
                    )
                }
            )
        }

        namedJdbc.update(
            """
                        UPDATE material_reservation
                        SET status = :status
                        WHERE direct_execution_id = :directExecutionId
                    """.trimIndent(),
            mapOf(
                "directExecutionId" to directExecutionId,
                "status" to ReservationStatus.FINISHED
            )
        )

        directExecutionRepository.finishDirectExecution(
            id = directExecutionId,
            status = ExecutionStatus.FINISHED,
            signatureUri = null,
            signDate = null,
            responsible = null
        )

        val installationItems = directExecutionRepositoryItem.getByDirectExecutionId(directExecutionId)
        installationItems.forEach {
            contractItemsQuantitativeRepository.updateBalance(
                it.contractItemId,
                it.measuredItemQuantity,
                it.factor ?: BigDecimal.ONE
            )
        }

        val streets = directExecutionRepositoryStreet.getByDirectExecutionId(directExecutionId)
        val cableDistribution =
            directExecutionRepositoryItem.getCableDistribution(streets.size, directExecutionId)

        streets.forEachIndexed { index, id ->
            val isLast = index == streets.lastIndex
            val quantity = if (isLast) {
                cableDistribution.first + cableDistribution.second
            } else {
                cableDistribution.first
            }

            directExecutionRepositoryStreetItem.save(
                DirectExecutionStreetItem(
                    executedQuantity = quantity,
                    materialStockId = null,
                    contractItemId = cableDistribution.third,
                    directExecutionStreetId = id
                )
            )
        }

        return ResponseEntity.ok().build()
    }

    @Transactional
    fun finishDirectExecutionV2(photo: MultipartFile?, request: InstallationRequest?): ResponseEntity<Any> {
        if (request == null) {
            throw Utils.BusinessException("Payload is null")
        }

        val directExecutionId = request.directExecutionId
        val installation = directExecutionRepository.getInstallation(directExecutionId)
        val description = installation?.description
        val status = installation?.status

        if (status == ExecutionStatus.FINISHED) {
            return ResponseEntity.noContent().build()
        }

        val fileUri =
            if (photo != null) {
                var folder = "photos"

                if (description != null) {
                    folder += "/${description.replace("\\s+".toRegex(), "_")}"
                }

                if (request.responsible != null) {
                    folder += "/${request.responsible.replace("\\s+".toRegex(), "_")}"
                }

                minioService.uploadFile(photo, Utils.getCurrentBucket(), folder, "installation")
            } else null

        directExecutionRepository.finishDirectExecution(
            id = directExecutionId,
            status = ExecutionStatus.FINISHED,
            signatureUri = fileUri,
            signDate = request.signDate,
            responsible = request.responsible
        )

        request.operationalUsers?.let { users ->
            directExecutionExecutorRepository.saveAll(
                users.map { userId ->
                    DirectExecutionExecutor(
                        directExecutionId = directExecutionId,
                        userId = userId,
                        isNewEntry = true
                    )
                }
            )
        }

        namedJdbc.update(
            """
                    UPDATE material_reservation
                    SET status = :status
                    WHERE direct_execution_id = :directExecutionId
                """.trimIndent(),
            mapOf(
                "directExecutionId" to directExecutionId,
                "status" to ReservationStatus.FINISHED
            )
        )

        val installationItems = directExecutionRepositoryItem.getByDirectExecutionId(directExecutionId)
        installationItems.forEach {
            contractItemsQuantitativeRepository.updateBalance(
                it.contractItemId,
                it.measuredItemQuantity,
                it.factor ?: BigDecimal.ONE
            )
        }

        val streets = directExecutionRepositoryStreet.getByDirectExecutionId(request.directExecutionId)
        val cableDistribution =
            directExecutionRepositoryItem.getCableDistribution(streets.size, request.directExecutionId)

        streets.forEachIndexed { index, id ->
            val isLast = index == streets.lastIndex
            val quantity = if (isLast) {
                cableDistribution.first + cableDistribution.second
            } else {
                cableDistribution.first
            }

            directExecutionRepositoryStreetItem.save(
                DirectExecutionStreetItem(
                    executedQuantity = quantity,
                    materialStockId = null,
                    contractItemId = cableDistribution.third,
                    directExecutionStreetId = id
                )
            )
        }

        return ResponseEntity.ok().build()
    }
}