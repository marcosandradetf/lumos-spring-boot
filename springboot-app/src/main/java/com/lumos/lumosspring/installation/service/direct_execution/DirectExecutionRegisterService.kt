package com.lumos.lumosspring.installation.service.direct_execution

import com.lumos.lumosspring.contract.repository.ContractItemDependencyRepository
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.contract.service.ContractService
import com.lumos.lumosspring.installation.controller.direct_execution.DirectExecutionRegisterController
import com.lumos.lumosspring.installation.dto.direct_execution.InstallationRequest
import com.lumos.lumosspring.installation.dto.direct_execution.InstallationStreetRequest
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecution
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecutionExecutor
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecutionStreet
import com.lumos.lumosspring.installation.model.direct_execution.DirectExecutionStreetItem
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionExecutorRepository
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepository
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryStreet
import com.lumos.lumosspring.installation.repository.direct_execution.DirectExecutionRepositoryStreetItem
import com.lumos.lumosspring.notifications.service.FCMService
import com.lumos.lumosspring.s3.service.S3Service
import com.lumos.lumosspring.scheduler.SchedulerService
import com.lumos.lumosspring.stock.materialsku.model.Material
import com.lumos.lumosspring.stock.materialstock.repository.MaterialStockRegisterRepository
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.NotificationType
import com.lumos.lumosspring.util.ReservationStatus
import com.lumos.lumosspring.util.Utils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
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
    private val contractService: ContractService,
    private val schedulerService: SchedulerService,
    private val fcmService: FCMService,
    private val contractReferenceItemRepository: ContractReferenceItemRepository
) {
    fun createInstallation(
        execution: DirectExecutionRegisterController.InstallationCreateRequest,
        teamId: Long
    ): ResponseEntity<Any> {
        val exists = directExecutionRepository.existsDirectExecutionByExternalIdAndTenantId(
            externalId = execution.directExecutionId,
            tenantId = Utils.getCurrentTenantId()
        )

        if (exists) {
            return ResponseEntity.noContent().build()
        }

        var step = 0
        val contract = if (execution.contractId != null) {
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
            directExecutionRepository.findDirectExecutionIdByExternalId(externalId = installationReq.directExecutionId)
                ?: throw Utils.BusinessException("Código externo dessa execução não encontrado.")
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
        val fileUri = s3Service.uploadFile(photo, folder, "installation", Utils.getCurrentTenantId())
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
            if (!m.truckStockControl) {
                continue
            }

            val balance = materialStockRegisterRepository.findStockQuantityByMaterialIdStock(m.truckMaterialStockId)
                .orElse(null)

            if (balance == null || balance < m.quantityExecuted) {
                throw Utils.BusinessException("Sem estoque para o material: " + m.truckMaterialStockId + " - " + m.materialName)
            }

            materialStockRegisterRepository.debitStock(
                m.quantityExecuted,
                m.truckMaterialStockId
            )

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
                contractItemId = if (m.contractItemId > 0) m.contractItemId else null,
                directExecutionStreetId = installationStreet.directExecutionStreetId
                    ?: throw IllegalStateException("directExecutionStreetId not setted")
            )

            directExecutionRepositoryStreetItem.save(item)

            if (item.contractItemId != null) {
                contractItemsQuantitativeRepository.updateBalance(
                    item.contractItemId!!,
                    item.executedQuantity
                )
                saveLinkedItems(item, directExecutionId, null)
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
            directExecutionRepository.findDirectExecutionIdByExternalId(externalId = request.directExecutionId)
                ?: throw Utils.BusinessException("Código externo dessa execução não encontrado.")
        }

        val installation = directExecutionRepository.getInstallation(directExecutionId)
        val description = installation?.description
        val status = installation?.status
        val contractId = installation?.contractId

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

                s3Service.uploadFile(photo, folder, "installation", Utils.getCurrentTenantId())
            } else null

        directExecutionRepository.finishDirectExecution(
            id = directExecutionId,
            status = if (request.directExecutionId > 0) ExecutionStatus.FINISHED else ExecutionStatus.AWAITING_COMPLETION,
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

        if (request.directExecutionId > 0) {
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

            this.fcmService.sendNotificationForTopic(
                title = "$description CONCLUÍDA",
                body = "A instalação foi finalizada com sucesso. Toque para gerar e visualizar o relatório.",
                notificationCode = "ANALISTA_${Utils.getCurrentTenantId()}",
                type = NotificationType.ALERT,
                platform = FCMService.TargetPlatform.WEB,
                isPopUp = false,
                uri = "/relatorios/gerenciamento?contractId=${contractId}&executionId=${directExecutionId}&type=data&scope=INSTALLATION&executionType=DIRECT_EXECUTION",
                relatedId = directExecutionId.toString(),
                subtitle = "Relatório disponível",
                tenant = Utils.getCurrentTenantId().toString()
            )

        } else {
            this.fcmService.sendNotificationForTopic(
                title = "Instalação concluída aguardando validação",
                body = "Para liberar o status, é necessário validar a execução vinculando os materiais aos seus respectivos itens contratuais.",
                notificationCode = "ANALISTA_${Utils.getCurrentTenantId()}",
                type = NotificationType.ALERT_BANNER,
                platform = FCMService.TargetPlatform.WEB,
                isPopUp = true,
                uri = "/contratos/validar-execucao/${directExecutionId}",
                relatedId = directExecutionId.toString(),
                subtitle = "Uma instalação foi concluída no campo sem ordem de serviço prévia.",
                tenant = Utils.getCurrentTenantId().toString()
            )
        }

        return ResponseEntity.ok().build()
    }

    private fun saveLinkedItems(
        item: DirectExecutionStreetItem,
        directExecutionId: Long?,
        contractId: Long?
    ): List<DirectExecutionStreetItem> {
        val itemDependency = if (directExecutionId != null) {
            contractItemDependencyRepository.getAllDirectExecutionItemsById(item.contractItemId!!, directExecutionId)
        } else if (contractId != null) {
            contractItemDependencyRepository.getAllRelatedContractItemsItemsById(item.contractItemId!!, contractId)
        } else {
            emptyList()
        }

        val dependencyItems: MutableList<DirectExecutionStreetItem> = mutableListOf()
        itemDependency.forEach { dependency ->
            val dependencyItem = item.copy(
                contractItemId = dependency.contractItemId,
                executedQuantity = item.executedQuantity,
                directExecutionStreetItemId = null,
                materialStockId = null
            )
            dependencyItems.add(dependencyItem)

            // somente debitar quando for origem de OS Gerada no sistema.
            if (directExecutionId != null) {
                directExecutionRepositoryStreetItem.save(dependencyItem)
                contractItemsQuantitativeRepository.updateBalance(
                    dependency.contractItemId,
                    item.executedQuantity
                )
            }
        }

        return dependencyItems
    }

    @Transactional
    fun preValidateExecution(req: DirectExecutionRegisterController.ReqValidation): ResponseEntity<Any> {
        val execution = directExecutionRepository.findById(req.directExecutionId).orElseThrow()

        if (execution.directExecutionStatus != ExecutionStatus.AWAITING_COMPLETION) {
            throw Utils.BusinessException("Execução solicitada não está com status de validação")
        }

        val linkedItemsResponse: MutableList<DirectExecutionStreetItem> = mutableListOf()
        val streetItems = directExecutionRepositoryStreetItem
            .findByDirectExecutionStreetItemIdIn(
                req.items.map { it.directExecutionStreetItemId }.toMutableList()
            )

        if (streetItems.size != req.items.size) {
            throw Utils.BusinessException("Alguns itens não foram encontrados")
        }

        val streetItemsMap = streetItems.associateBy { it.directExecutionStreetItemId }

        val contractItemIds = req.items
            .map { it.contractItemId }
            .distinct()

        val reservedQuantityItems =
            contractService.getExecutedQuantityByContract(
                contractItemIds.toMutableList(),
                true
            )

        val contractItems = contractItemsQuantitativeRepository
            .findByContractItemIdIn(contractItemIds)

        val reservedMap = reservedQuantityItems.groupBy { it.contractItemId }
        val contractItemsMap = contractItems
            .associateBy { it.contractItemId }

        req.items.forEach { item ->
            val streetItem =
                streetItemsMap[item.directExecutionStreetItemId]
                    ?: throw Utils.BusinessException("Item da execução não encontrado")

            val contractItem = contractItemsMap[item.contractItemId]
                ?: throw Utils.BusinessException("Item do contrato não encontrado")

            val reserved =
                reservedMap[item.contractItemId]?.sumOf { it.quantity }
                    ?: BigDecimal.ZERO

            val balance = contractItem.contractedQuantity - (contractItem.quantityExecuted + reserved)

            if (balance < streetItem.executedQuantity) {
                throw Utils.BusinessException("Quantidade indisponível")
            }

            streetItem.contractItemId = item.contractItemId

            val linkedItems = saveLinkedItems(streetItem, null, req.contractId)
            linkedItemsResponse.addAll(linkedItems)
            streetItems.addAll(
                linkedItems.map { linkedItem ->
                    val material = Material()
                    val description = contractReferenceItemRepository
                        .getDescription(linkedItem.contractItemId).orElse(null)
                        ?: throw Utils.BusinessException(
                            "Descrição do item vinculado não encontrada"
                        )
                    material.materialName = description
                    linkedItem.material = material
                    linkedItem
                }
            )

            reservedMap[streetItem.contractItemId]
                ?.getOrNull(0)
                ?.quantity += streetItem.executedQuantity
        }

        execution.contractId = req.contractId
        execution.step = contractRepository.getLastStep(execution.contractId!!) + 1
        execution.directExecutionStatus = ExecutionStatus.VALIDATING

        directExecutionRepositoryStreetItem.saveAll(streetItems)
        directExecutionRepository.save(execution)

        schedulerService.scheduleAutoConfirm(
            execution.directExecutionId!!,
            Utils.getCurrentTenantId(),
            Instant.now().plusSeconds(598)
        )

        return ResponseEntity.ok().body(
            linkedItemsResponse
        )
    }

    @Transactional
    fun cancelValidation(executionId: Long, streetItemIds: List<Long>) {
        schedulerService.cancelAutoConfirm(executionId)

        val execution = directExecutionRepository.findById(executionId).orElseThrow()
        execution.contractId = null
        execution.step = 0
        execution.directExecutionStatus = ExecutionStatus.AWAITING_COMPLETION

        val streetItems = directExecutionRepositoryStreetItem
            .findByDirectExecutionStreetItemIdIn(streetItemIds.toMutableList())

        val itemsToDelete = streetItems.filter { it.materialStockId == null }
        val itemsToUpdate = streetItems.filter { it.materialStockId != null }

        itemsToUpdate.forEach {
            it.contractItemId = null
        }

        directExecutionRepositoryStreetItem.saveAll(itemsToUpdate)
        directExecutionRepositoryStreetItem.deleteAll(itemsToDelete)

        directExecutionRepository.save(execution)
    }

    @Transactional
    fun deleteItem(itemId: Long): ResponseEntity<Any> {
        val streetItem = directExecutionRepositoryStreetItem.findById(itemId).orElseThrow()
        streetItem.contractItemId = null
        directExecutionRepositoryStreetItem.save(streetItem)

        return ResponseEntity.ok().build()
    }


    @Transactional
    fun confirmPreparedExecution(executionId: Long): ResponseEntity<Any> {
        schedulerService.cancelAutoConfirm(executionId)
        validateExecution(executionId)
        return ResponseEntity.ok().build()
    }

    @Transactional
    fun validateExecution(executionId: Long) {
        val execution = directExecutionRepository.findById(executionId).orElseThrow()

        if (execution.directExecutionStatus != ExecutionStatus.VALIDATING) {
            throw Utils.BusinessException(
                "Execução solicitada não está com status de confirmação. Atualize a tela e tente novamente."
            )
        }

        val streets = directExecutionRepositoryStreet.findAllByDirectExecutionId(executionId)
        val streetItems = directExecutionRepositoryStreetItem
            .findAllByDirectExecutionStreetIdIn(
                streets.map { it.directExecutionStreetId!! }.toMutableList()
            )

        val contract = contractRepository.findById(execution.contractId!!).orElseThrow()
        val contractItemIds = streetItems
            .mapNotNull { it.contractItemId }
            .distinct()

        val reservedQuantityItems =
            contractService.getExecutedQuantityByContract(
                contractItemIds.toMutableList(),
                true
            )

        val contractItems = contractItemsQuantitativeRepository
            .findByContractItemIdIn(contractItemIds)

        val reservedMap = reservedQuantityItems.groupBy { it.contractItemId }
        val contractItemsMap = contractItems
            .associateBy { it.contractItemId }

        streetItems
            .filter { it.contractItemId != null }
            .forEach { streetItem ->
                val contractItem = contractItemsMap[streetItem.contractItemId]

                if (contractItem == null) {
                    cancelValidation(
                        executionId = execution.directExecutionId!!,
                        streetItemIds = streetItems.mapNotNull { it.directExecutionStreetItemId }
                    )
                    throw Utils.BusinessException("Item do contrato não encontrado")
                }

                reservedMap[streetItem.contractItemId]
                    ?.find { it.step == execution.step && it.installationId == execution.directExecutionId }
                    ?.quantity -= streetItem.executedQuantity

                val reserved =
                    reservedMap[streetItem.contractItemId]?.sumOf { it.quantity }
                        ?: BigDecimal.ZERO

                val balance = contractItem.contractedQuantity - (contractItem.quantityExecuted + reserved)

                if (balance < streetItem.executedQuantity) {
                    cancelValidation(
                        executionId = execution.directExecutionId!!,
                        streetItemIds = streetItems.mapNotNull { it.directExecutionStreetItemId }
                    )
                    throw Utils.BusinessException("Quantidade indisponível")
                }

                println("Item ${contractItem.executedQuantity} - Referencia: ${contractItem.referenceItemId}: Quantidade Executada: ${contractItem.quantityExecuted}")
                contractItem.quantityExecuted += streetItem.executedQuantity
                println("Item ${contractItem.executedQuantity} - Referencia: ${contractItem.referenceItemId}: Quantidade Executada: ${contractItem.quantityExecuted}")
            }

        execution.directExecutionStatus = ExecutionStatus.FINISHED
        execution.description = "Etapa " + execution.step + " - " + contract.contractor

        try {
            contractItemsQuantitativeRepository.saveAll(contractItems)

            directExecutionRepository.save(execution)

            // excui os itens que foram excluídos
            directExecutionRepositoryStreetItem.deleteAll(
                streetItems
                    .filter { it.contractItemId == null }
            )
        } catch (_: OptimisticLockingFailureException) {
            cancelValidation(
                executionId = execution.directExecutionId!!,
                streetItemIds = streetItems.mapNotNull { it.directExecutionStreetItemId }
            )
            throw Utils.BusinessException(
                "O saldo do contrato foi alterado por outra operação. Atualize a tela e tente novamente."
            )
        } catch (e: Exception) {
            cancelValidation(
                executionId = execution.directExecutionId!!,
                streetItemIds = streetItems.mapNotNull { it.directExecutionStreetItemId }
            )
            throw Utils.BusinessException(e.message)
        }

        println("schedule finalizado")
    }


}