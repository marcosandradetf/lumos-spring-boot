package com.lumos.lumosspring.installation.service.direct_execution

import com.lumos.lumosspring.contract.repository.ContractItemDependencyRepository
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.installation.controller.direct_execution.DirectExecutionRegisterController
import com.lumos.lumosspring.installation.dto.direct_execution.InstallationRequest
import com.lumos.lumosspring.installation.dto.direct_execution.InstallationStreetRequest
import com.lumos.lumosspring.installation.dto.premeasurement.InstallationResponse
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecution
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecutionExecutor
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecutionStreet
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecutionStreetItem
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionExecutorRepository
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepository
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryStreet
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryStreetItem
import com.lumos.lumosspring.s3.service.S3Service
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository
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
import java.time.Instant

@Service
class DirectExecutionRegisterService(
    private val s3Service: S3Service,
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val directExecutionRepositoryStreet: DirectExecutionRepositoryStreet,
    private val directExecutionRepositoryStreetItem: DirectExecutionRepositoryStreetItem,
    private val directExecutionExecutorRepository: DirectExecutionExecutorRepository,
    private val directExecutionRepository: DirectExecutionRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val contractItemDependencyRepository: ContractItemDependencyRepository,
    private val materialStockRegisterRepository: MaterialStockRegisterRepository,
    private val contractRepository: ContractRepository,
) {
    fun createInstallation(
        execution: DirectExecutionRegisterController.InstallationCreateRequest,
        teamId: Long
    ): ResponseEntity<Any> {
        val exists = directExecutionRepository.existsDirectExecutionByExternalIdAndTenantId(
            externalId = execution.directExecutionId,
            tenantId = Utils.getCurrentTenantId()
        )

        if(exists) {
           return ResponseEntity.noContent().build()
        }

        var step = 0
        val contract = if(execution.contractId != null) {
            step = contractRepository.getLastStep(execution.contractId!!) + 1
            contractRepository.findById(execution.contractId!!).orElseThrow()
        } else null

        val directExecution = DirectExecution(
            null,
            if (contract == null) execution.description else "Etapa " + step + " - " + contract.contractor,
            execution.instructions,
            execution.contractId,
            ExecutionStatus.DRAFT,
            teamId,
            Utils.getCurrentUserId(),
            Instant.now(),
            null,
            step,
            null,
            execution.creationDate,
            null,
            execution.creationDate,
            execution.directExecutionId
        )
        directExecutionRepository.save(directExecution)

        return ResponseEntity.noContent().build()

    }


    @Transactional
    fun saveStreetInstallationV2(
        photo: MultipartFile,
        installationReq: InstallationStreetRequest?
    ): ResponseEntity<Any> {
        if (installationReq == null) {
            return ResponseEntity.badRequest().body("payload vazio.")
        }

        val exists = directExecutionRepositoryStreet.existsDirectExecutionStreetByDeviceStreetIdAndDeviceId(
            deviceStreetId = installationReq.deviceStreetId,
            deviceId = installationReq.deviceId,
        )

        if (exists) {
            return ResponseEntity.status(200).body("Rua já enviada antes ou instalação já finalizada!")
        }

        val directExecutionId = if (installationReq.directExecutionId > 0) {
            // id generated by system
            installationReq.directExecutionId
        } else {
            // external id (mobile local generated)
            // and update status on first street
            directExecutionRepository.findDirectExecutionIdByExternalId(externalId = installationReq.directExecutionId) ?:
                throw Utils.BusinessException("Código externo dessa execução não encontrado.")
        }

        var installationStreet = DirectExecutionStreet(
            lastPower = installationReq.lastPower,
            address = installationReq.address,
            latitude = installationReq.latitude,
            longitude = installationReq.longitude,
            deviceStreetId = installationReq.deviceStreetId,
            deviceId = installationReq.deviceId,
            finishedAt = installationReq.finishAt,
            directExecutionId = directExecutionId,
            currentSupply = installationReq.currentSupply,
            comment = installationReq.comment
        )

        val folder = "photos/${installationReq.description.replace("\\s+".toRegex(), "_")}"
        val fileUri = s3Service.uploadFile(photo, folder, "installation",Utils.getCurrentTenantId())
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
            val balance = materialStockRegisterRepository.findStockQuantityByMaterialIdStock(m.truckMaterialStockId)
                .orElse(null)

            if (balance == null || balance < m.quantityExecuted) {
                throw Utils.BusinessException("Sem estoque para o material: " + m.truckMaterialStockId + " - " + m.materialName)
            }

            if (m.truckStockControl) {
                materialStockRegisterRepository.debitStock(
                    m.quantityExecuted,
                    m.truckMaterialStockId
                )
            }

            if (m.reserveId >= 0) {
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
            }

            val item = DirectExecutionStreetItem(
                executedQuantity = m.quantityExecuted,
                materialStockId = m.truckMaterialStockId,
                contractItemId = if(m.contractItemId > 0) m.contractItemId else null,
                directExecutionStreetId = installationStreet.directExecutionStreetId
                    ?: throw IllegalStateException("directExecutionStreetId not setted")
            )

            directExecutionRepositoryStreetItem.save(item)

            if (item.contractItemId != null) {
                contractItemsQuantitativeRepository.updateBalance(
                    item.contractItemId!!,
                    item.executedQuantity
                )
                saveLinkedItems(item, directExecutionId)
            }

        }

        return ResponseEntity.ok().build()
    }

    @Transactional
    fun finishDirectExecutionV2(photo: MultipartFile?, request: InstallationRequest?): ResponseEntity<Any> {
        if (request == null) {
            throw Utils.BusinessException("Payload is null")
        }

        val directExecutionId = if (request.directExecutionId > 0) {
            // id generated by system
            request.directExecutionId
        } else {
            // external id (mobile local generated)
            directExecutionRepository.findDirectExecutionIdByExternalId(externalId = request.directExecutionId) ?:
                throw Utils.BusinessException("Código externo dessa execução não encontrado.")
        }

        val installation = directExecutionRepository.getInstallation(directExecutionId)
        val description = installation?.description
        val status = installation?.status

        if (status == ExecutionStatus.FINISHED || status == ExecutionStatus.AWAITING_COMPLETION) {
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

                s3Service.uploadFile(photo,  folder, "installation", Utils.getCurrentTenantId())
            } else null

        directExecutionRepository.finishDirectExecution(
            id = directExecutionId,
            status = if(request.directExecutionId > 0) ExecutionStatus.FINISHED else ExecutionStatus.AWAITING_COMPLETION,
            signatureUri = fileUri,
            signDate = request.signDate,
            finishedAt = request.signDate ?: Instant.now(),
            responsible = request.responsible,
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

        if(directExecutionId > 0) {
            namedJdbc.update(
                """
                    UPDATE material_reservation
                    SET status = :status, finished_at = now()
                    WHERE direct_execution_id = :directExecutionId
                """.trimIndent(),
                mapOf(
                    "directExecutionId" to directExecutionId,
                    "status" to ReservationStatus.FINISHED
                )
            )
        }
        return ResponseEntity.ok().build()
    }

    private fun saveLinkedItems(item: DirectExecutionStreetItem, directExecutionId: Long) {
        val itemDependency =
            contractItemDependencyRepository.getAllDirectExecutionItemsById(item.contractItemId!!, directExecutionId)

        itemDependency.forEach { dependency ->
            val dependencyItem = item.copy(
                contractItemId = dependency.contractItemId,
                executedQuantity = item.executedQuantity * dependency.factor,
                directExecutionStreetItemId = null,
                materialStockId = null
            )
            directExecutionRepositoryStreetItem.save(dependencyItem)
            contractItemsQuantitativeRepository.updateBalance(
                dependency.contractItemId,
                item.executedQuantity * dependency.factor
            )
        }
    }
}