package com.lumos.lumosspring.directexecution.service

import com.lumos.lumosspring.directexecution.dto.InstallationRequest
import com.lumos.lumosspring.directexecution.dto.InstallationStreetRequest
import com.lumos.lumosspring.directexecution.model.DirectExecutionExecutor
import com.lumos.lumosspring.directexecution.model.DirectExecutionStreet
import com.lumos.lumosspring.directexecution.model.DirectExecutionStreetItem
import com.lumos.lumosspring.directexecution.repository.DirectExecutionExecutorRepository
import com.lumos.lumosspring.directexecution.repository.DirectExecutionRepository
import com.lumos.lumosspring.directexecution.repository.DirectExecutionRepositoryStreet
import com.lumos.lumosspring.directexecution.repository.DirectExecutionRepositoryStreetItem
import com.lumos.lumosspring.minio.service.MinioService
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.JdbcUtil
import com.lumos.lumosspring.util.JdbcUtil.getRawData
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

            var balance = namedJdbc.queryForObject(
                """
                    select contracted_quantity - quantity_executed as balance
                    from contract_item
                    where contract_item_id = :contractItemId
                    limit 1
                """.trimIndent(),
                mapOf("contractItemId" to m.contractItemId),
                BigDecimal::class.java
            )

            if (balance == null || balance < m.quantityExecuted) {
                throw Utils.BusinessException("Sem saldo contratual para o material: " + m.contractItemId + " - " + m.materialName)
            }

            balance = namedJdbc.queryForObject(
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
                throw Utils.BusinessException("Sem estoque para o material: " + m.contractItemId + " - " + m.materialName)
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

            val hasService = when {
                m.materialName.contains("led", ignoreCase = true) -> "led"
                m.materialName.contains("braço", ignoreCase = true) -> "braço"
                else -> null
            }

            val params = mutableMapOf<String, Any?>(
                "quantityExecuted" to m.quantityExecuted,
                "contractItemId" to m.contractItemId
            )

            hasService?.let {
                params["dependency"] = it
                params["directExecutionId"] = installationReq.directExecutionId
            }

            if (hasService != null) {
                val servicesData: List<Map<String, Any>> =
                    getRawData(
                        namedJdbc,
                        """
                            WITH to_update AS (
                                SELECT ci.contract_item_id, false as isService, null as factor
                                FROM contract_item ci
                                WHERE ci.contract_item_id = :contractItemId

                                UNION ALL

                                SELECT ci.contract_item_id, true as isService, cri.factor
                                FROM contract_item ci
                                JOIN contract_reference_item cri ON cri.contract_reference_item_id = ci.contract_item_reference_id
                                JOIN direct_execution_item di ON di.contract_item_id = ci.contract_item_id
                                WHERE lower(cri.item_dependency) = :dependency
                                    AND lower(cri.type) IN ('projeto', 'serviço', 'cemig')
                                    AND di.direct_execution_id = :directExecutionId
                            )
                            UPDATE contract_item ci
                            SET quantity_executed = case 
                                                        when tu.factor is null then quantity_executed + :quantityExecuted 
                                                        else quantity_executed + :quantityExecuted * tu.factor
                                                    end
                            FROM to_update tu
                            WHERE ci.contract_item_id = tu.contract_item_id
                            RETURNING ci.contract_item_id, tu.isService
                        """.trimIndent(),
                        params
                    )

                for (s in servicesData) {
                    val serviceItemId = (s["contract_item_id"] as Number).toLong()
                    val isService = s["isService"] as Boolean

                    if (!isService) continue

                    val serviceItem = DirectExecutionStreetItem(
                        executedQuantity = m.quantityExecuted,
                        materialStockId = null,
                        contractItemId = serviceItemId,
                        directExecutionStreetId = installationStreet.directExecutionStreetId
                            ?: throw IllegalStateException("directExecutionStreetId not set")
                    )
                    directExecutionRepositoryStreetItem.save(serviceItem)
                }

            } else {

                // Use update porque não há retorno
                namedJdbc.update(
                    """
                            UPDATE contract_item
                            SET quantity_executed = quantity_executed + :quantityExecuted
                            WHERE contract_item_id = :contractItemId
                    """.trimIndent(),
                    params
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

        return ResponseEntity.ok().build()
    }
}