package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.contract.controller.ContractController
import com.lumos.lumosspring.contract.dto.ContractDTO
import com.lumos.lumosspring.contract.dto.ContractReferenceItemDTO
import com.lumos.lumosspring.contract.dto.PContractReferenceItemDTO
import com.lumos.lumosspring.contract.entities.Contract
import com.lumos.lumosspring.contract.entities.ContractItem
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractReferenceItemRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.notifications.service.Routes
import com.lumos.lumosspring.s3.service.S3Service
import com.lumos.lumosspring.system.entities.Log
import com.lumos.lumosspring.system.repository.LogRepository
import com.lumos.lumosspring.user.model.AppUser
import com.lumos.lumosspring.user.repository.UserRepository
import com.lumos.lumosspring.util.*
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class ContractService(
    private val contractRepository: ContractRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
    private val contractReferenceItemRepository: ContractReferenceItemRepository,
    private val util: Util,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val s3Service: S3Service,
    private val logRepository: LogRepository,
) {
    @Transactional
    fun deleteById(contractId: Long): ResponseEntity<Any> {
        try {
            contractItemsQuantitativeRepository.deleteByContractId((contractId))
            contractRepository.deleteById(contractId)
        } catch (_: Exception) {
            throw IllegalStateException("Como este contrato já possui registros vinculados (como Manutenções, instalações já executadas ou Ordens de Serviços pendentes), a exclusão não é permitida.")
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Caching(
        evict = [CacheEvict(
            cacheNames = ["GetContractsForPreMeasurement"],
            key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
        )]
    )
    @Transactional
    fun saveContract(contractDTO: ContractDTO): ResponseEntity<Any> {
        return if (contractDTO.contractId != null) {
            updateContract(contractDTO)
        } else {
            createContract(contractDTO)
        }
    }

    @Transactional
    fun updateItems(
        contractId: Long,
        items: List<ContractReferenceItemDTO>
    ): ResponseEntity<Any> {
        val user = userRepository.findByUserId(Utils.getCurrentUserId()).orElseThrow()
        val contract = contractRepository.findById(contractId).orElseThrow()

        changeItems(
            user = user,
            contract = contract,
            items = items
        )

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    private fun updateContract(contractDTO: ContractDTO): ResponseEntity<Any> {
        val contractId = contractDTO.contractId!!
        val contract = contractRepository.findById(contractId).orElseThrow()
        val user = userRepository.findByUserId(Utils.getCurrentUserId()).orElseThrow()

        contract.contractNumber = contractDTO.number
        contract.phone = contractDTO.phone
        contract.contractor = contractDTO.contractor
        contract.cnpj = contractDTO.cnpj
        contract.address = contractDTO.address
        contract.unifyServices = contractDTO.unifyServices
        contract.lastUpdatedBy = user.userId

        if ((contractDTO.contractFile?.length ?: 0) > 0) {
            val filesToDelete = buildSet {
                contract.contractFile
                    ?.let { add(it) }

                contract.noticeFile
                    ?.let { add(it) }
            }

            if (filesToDelete.isNotEmpty()) {
                s3Service.deleteFiles(Utils.getCurrentBucket(), filesToDelete)
            }

            contract.contractFile = contractDTO.contractFile
            contract.noticeFile = contractDTO.noticeFile
        } else {
            val filesToDelete = buildSet {
                contract.noticeFile
                    ?.let { add(it) }
            }

            if (filesToDelete.isNotEmpty()) {
                s3Service.deleteFiles(Utils.getCurrentBucket(), filesToDelete)
            }
        }

        contract.companyId = contractDTO.companyId
        contractRepository.save(contract)

        changeItems(
            user = user,
            contract = contract,
            items = contractDTO.items
        )

        return ResponseEntity.ok(DefaultResponse("Contrato atualizado com sucesso!"))
    }

    private fun changeItems(
        user: AppUser,
        contract: Contract,
        items: List<ContractReferenceItemDTO>,
    ) {
        val contractId = contract.contractId
        val log = Log()

        log.message = """
            Usuário ${user.username} alterou o contrato ${contract.contractNumber} - ${contract.contractor}
        """.trimIndent()
        log.category = "contract"
        log.type = "update"
        log.creationTimestamp = Instant.now()
        log.idUser = user.userId
        logRepository.save(log)

        val currentContractItems = contractItemsQuantitativeRepository.findAllByContractId(contractId)
        val deleteContractItemsIds =
            currentContractItems
                .filterNot { currentItem ->
                    items.filter { it.contractItemId != null }
                        .map { it.contractItemId }.contains(currentItem.contractItemId)
                }
                .map { it.contractItemId }

        if(deleteContractItemsIds.isNotEmpty()) {
            val blockedItems = contractItemsQuantitativeRepository
                .findBlockedItemDescriptions(deleteContractItemsIds)

            if (blockedItems.isNotEmpty()) {
                val descriptions = blockedItems.joinToString { it }
                val message = if (blockedItems.size > 1) {
                    "Não é possível excluir os itens: $descriptions, pois já estão vinculados a registros de instalações ou O.S. no sistema."
                } else {
                    "Não é possível excluir o item: $descriptions, pois já está vinculado a um registro de instalação ou O.S. no sistema."
                }

                throw Utils.BusinessException(message)
            }

            try {
                contractItemsQuantitativeRepository.deleteAllById(deleteContractItemsIds)
            } catch (ex: Exception) {
                ex.printStackTrace()

                throw Utils.BusinessException(
                    "Erro ao excluir itens: ${ex.message}"
                )
            }
        }

        val reservedQuantityByItem = contractItemsQuantitativeRepository
            .getReservedQuantity(
                items.map { it.contractItemId }
            )

        items.forEach { item ->
            val ci = if (item.contractItemId != null) {
                currentContractItems.find { it.contractItemId == item.contractItemId }
                    ?: throw Utils.BusinessException("Não foi possível encontrar o item ${item.description} na coleção")
            } else {
                ContractItem()
            }

            val reservedQuantity = reservedQuantityByItem
                .find { it.contractItemId() == item.contractItemId }?.reservedQuantity ?: BigDecimal.ZERO
            val minQuantity = ci.quantityExecuted + reservedQuantity

            if (item.contractItemId != null && item.quantity!! < minQuantity) {
                throw Utils.BusinessException(
                    "A quantidade informada para o item ${item.description} (${item.quantity}) é inferior ao mínimo permitido ($minQuantity). " +
                            "O valor mínimo corresponde ao total já executado ou reservado em O.S."
                )
            }

            ci.referenceItemId = item.contractReferenceItemId
            ci.contractId = contractId
            ci.contractedQuantity = item.quantity!!
            ci.setPrices(item.price)
            contractItemsQuantitativeRepository.save(ci)
        }
    }

    private fun createContract(contractDTO: ContractDTO): ResponseEntity<Any> {
        var contract = Contract()
        val user = userRepository.findByUserId(Utils.getCurrentUserId())
            ?: throw IllegalStateException("Usuário não encontrado")

        contract.contractNumber = contractDTO.number
        contract.contractor = contractDTO.contractor
        contract.cnpj = contractDTO.cnpj
        contract.address = contractDTO.address
        contract.phone = contractDTO.phone
        contract.createdBy = user.get().userId
        contract.unifyServices = contractDTO.unifyServices
        contract.contractFile = if ((contractDTO.contractFile?.length ?: 0) > 0) contractDTO.contractFile else null
        contract.companyId = contractDTO.companyId

        contract = contractRepository.save(contract)

        contractDTO.items.forEach { item ->
            val ci = ContractItem()
            ci.referenceItemId = item.contractReferenceItemId
            ci.contractId = contract.contractId
            ci.contractedQuantity = item.quantity!!
            ci.setPrices(item.price)
            contractItemsQuantitativeRepository.save(ci)
        }

        for (notificationCode in userRepository.getResponsibleTechUsers(Utils.getCurrentTenantId())) {
            notificationService.sendNotificationForTopic(
                title = "Novo contrato pendente para Pré-Medição",
                body = "Colaboradora ${user.get().name} criou o contrato de ${contract.contractor}",
                action = Routes.CONTRACT_SCREEN,
                notificationCode = notificationCode.toString(),
                type = NotificationType.CONTRACT
            )
        }

        return ResponseEntity.ok(DefaultResponse("Contrato salvo com sucesso!"))
    }

    fun getContract(contractId: Long): ResponseEntity<Any> {
        data class ItemsForReport(
            val number: Int,
            val contractItemId: Long,
            val description: String,
            val unitPrice: BigDecimal,
            val contractedQuantity: Double,
            val linking: String?,
        )

        data class ContractForReport(
            val contractId: Long,
            val number: String,
            val contractor: String,
            val cnpj: String,
            val phone: String,
            val address: String,
            val contractFile: String?,
            val createdBy: String,
            val createdAt: String,
            val items: List<ItemsForReport>,
        )

        val contract = contractRepository.findContractByContractId(contractId).orElseThrow()

        val items = queryContractItems(contractId).sortedBy { it["description"] as String }
            .mapIndexed { index, item ->
                ItemsForReport(
                    number = index + 1,
                    contractItemId = item["contract_item_id"] as Long,
                    description = item["description"] as String,
                    unitPrice = item["unit_price"] as BigDecimal,
                    contractedQuantity = (item["contracted_quantity"] as Number).toDouble(),
                    linking = item["linking"] as String?,
                )
            }

        val user = userRepository.findByUserId(contract.createdBy).orElseThrow()

        return ResponseEntity.ok(
            ContractForReport(
                contractId = contract.contractId!!,
                number = contract.contractNumber ?: "",
                contractor = contract.contractor ?: "",
                cnpj = contract.cnpj ?: "",
                phone = contract.phone ?: "",
                address = contract.address ?: "",
                contractFile = contract.contractFile,
                createdBy = user.completedName,
                createdAt = contract.creationDate.toString(),
                items = items
            )
        )
    }

    fun queryContractItems(contractId: Long): List<Map<String, Any>> {
        return JdbcUtil.getRawData(
            namedJdbc = namedJdbc,
            """
                select ci.contract_item_id, cri.description, ci.unit_price, ci.contracted_quantity,
                ci.quantity_executed, cri.linking, cri.name_for_import, cri.type, cri.contract_reference_item_id
                from contract_item ci
                join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
                where ci.contract_contract_id = :contractId
            """.trimIndent(),
            mapOf("contractId" to contractId)
        )
    }

    fun getAllActiveContracts(filters: ContractController.FilterRequest): ResponseEntity<Any> {
        val endPlusOneDay = filters.endDate?.plus(1, ChronoUnit.DAYS)
        val contractor = filters.contractor

        val response = contractRepository.findAllByTenantIdAndStatus(
            Utils.getCurrentTenantId(),
            filters.status,
            filters.startDate,
            endPlusOneDay,
            if (contractor == "") null else contractor?.lowercase()
        )

        return ResponseEntity.ok()
            .body(response)
    }

    fun getContractItems(contractId: Long): ResponseEntity<Any> {
        data class ContractItemsResponse(
            val number: Int,
            val contractItemId: Long,
            val description: String,
            val unitPrice: String,
            val contractedQuantity: BigDecimal,
            val executedQuantity: BigDecimal,
            val reservedQuantity: BigDecimal,
            val linking: String?,
            val nameForImport: String?,
            val type: String
        )

        val contractItems = JdbcUtil.getRawData(
            namedJdbc = namedJdbc,
            """
                select ci.contract_item_id, cri.description, ci.unit_price, ci.contracted_quantity,
                ci.quantity_executed, cri.linking, cri.name_for_import, cri.type, 
                (
                    select coalesce(sum(reserved_quantity - quantity_completed),0)
                    from material_reservation 
                    where contract_item_id = ci.contract_item_id
                        and status <> 'FINISHED' and quantity_completed < reserved_quantity
        	    ) as reserved_quantity
                from contract_item ci
                join contract_reference_item cri on cri.contract_reference_item_id = ci.contract_item_reference_id
                where ci.contract_contract_id = :contractId
            """.trimIndent(),
            mapOf("contractId" to contractId)
        )

        return ResponseEntity.ok().body(
            contractItems
                .sortedBy { it["description"] as String }
                .mapIndexed { index, it ->
                    ContractItemsResponse(
                        number = index + 1,
                        contractItemId = it["contract_Item_Id"] as Long,
                        description = it["description"] as String,
                        unitPrice = (it["unit_Price"] as BigDecimal).toPlainString(),
                        contractedQuantity = BigDecimal(it["contracted_Quantity"].toString()),
                        executedQuantity = BigDecimal(it["quantity_Executed"].toString()),
                        reservedQuantity = BigDecimal(it["reserved_quantity"].toString()),
                        linking = it["linking"] as? String,
                        nameForImport = it["name_For_Import"] as? String,
                        type = it["type"] as String,
                    )
                })
    }

    @Cacheable(
        value = ["GetContractsForPreMeasurement"],
        key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
    )
    fun getContractsForPreMeasurement(): ResponseEntity<Any> {
        data class ContractForPreMeasurementDTO(
            val contractId: Long,
            val contractor: String,
            val contractFile: String?,
            val createdBy: String,
            val createdAt: String,
            val status: String,
            val itemsIds: String? = null,
            val hasMaintenance: Boolean
        )

        val contractList = namedJdbc.query(
            """
                SELECT 
                    c.contract_id, 
                    c.contractor, 
                    c.contract_file, 
                    u.name || ' ' || u.last_name AS created_by,
                    c.creation_date,
                    c.status
                FROM contract c
                JOIN app_user u ON u.user_id = c.created_by_id_user
                WHERE c.status = 'ACTIVE'
                    AND c.tenant_id = :tenantId
            """.trimIndent(),
            mapOf("tenantId" to Utils.getCurrentTenantId()) // Nenhum parâmetro necessário aqui
        ) { rs, _ ->
            var hasMaintenance = false

            val itemsIds: String = namedJdbc.query(
                """
                        SELECT cri.contract_reference_item_id, cri.description
                        FROM contract_item ci
                        JOIN contract_reference_item cri ON cri.contract_reference_item_id = ci.contract_item_reference_id
                        WHERE contract_contract_id = :contractId
                    """.trimIndent(),
                mapOf("contractId" to rs.getLong("contract_id"))
            ) { cRs, _ ->
                if (cRs.getString("description").contains("manuten", true)) {
                    hasMaintenance = true
                }
                cRs.getLong("contract_reference_item_id")
            }.joinToString("#")

            ContractForPreMeasurementDTO(
                contractId = rs.getLong("contract_id"),
                contractor = rs.getString("contractor"),
                contractFile = rs.getString("contract_file"),
                createdBy = rs.getString("created_by"),
                createdAt = rs.getString("creation_date"),
                status = rs.getString("status"),
                itemsIds = itemsIds,
                hasMaintenance = hasMaintenance
            )
        }

        return ResponseEntity.ok(contractList)
    }


    @Cacheable(
        value = ["GetItemsForMobPreMeasurement"],
        key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
    )
    fun getItemsForMob(): ResponseEntity<List<PContractReferenceItemDTO>> {
        val items = contractReferenceItemRepository.findAllByTenantId(Utils.getCurrentTenantId())
            .map {
                PContractReferenceItemDTO(
                    contractReferenceItemId = it.contractReferenceItemId!!,
                    description = it.description,
                    nameForImport = it.nameForImport ?: it.description,
                    type = it.type,
                    linking = it.linking,
                    itemDependency = it.itemDependency,
                )
            }

        val sortedItems = items.sortedWith(
            compareBy<PContractReferenceItemDTO> { it.description }
                .thenBy { util.extractNumber(it.linking ?: "") }
        )
        return ResponseEntity.ok(sortedItems)
    }

    fun getContractItemsWithExecutionsSteps(contractId: Long): ResponseEntity<Any> {
        data class ContractItemsResponseWithExecutions(
            val contractReferenceItemId: Long,
            val number: Int,
            val contractItemId: Long,
            val description: String,
            val unitPrice: BigDecimal,
            val contractedQuantity: BigDecimal,
            val executedQuantity: List<ExecutedQuantity>,
            val totalExecuted: BigDecimal,
            val reservedQuantity: List<ExecutedQuantity>,
            val linking: String?,
            val nameForImport: String?,
            val type: String
        )

        return ResponseEntity.ok().body(
            queryContractItems(contractId)
                .sortedWith(
                    compareByDescending<Map<String, Any>> {
                        (it["quantity_executed"] as Number).toDouble()
                    }.thenBy {
                        it["description"] as? String ?: ""
                    }
                )
                .mapIndexed { index, it ->
                    ContractItemsResponseWithExecutions(
                        contractReferenceItemId = it["contract_reference_item_id"] as Long,
                        number = index + 1,
                        contractItemId = it["contract_item_id"] as Long,
                        description = it["description"] as String,
                        unitPrice = it["unit_price"] as BigDecimal,
                        contractedQuantity = it["contracted_quantity"] as BigDecimal,
                        executedQuantity = getExecutedQuantityByContract(it["contract_item_id"] as Long),
                        reservedQuantity = getExecutedQuantityByContract(it["contract_item_id"] as Long, true),
                        totalExecuted = it["quantity_executed"] as BigDecimal,
                        linking = it["linking"] as? String,
                        nameForImport = it["name_for_import"] as? String,
                        type = it["type"] as String
                    )
                }
        )
    }

    data class ExecutedQuantity(
        val installationId: Long,
        val step: Int,
        val quantity: BigDecimal,
    )

    private fun getExecutedQuantityByContract(contractItemId: Long, forReserve: Boolean = false): List<ExecutedQuantity> {
        val sql = if(forReserve) {
            """
                SELECT
                    t.installation_id,
                    t.step,
                    SUM(t.measured_item_quantity) AS quantity
                FROM (
                    SELECT
                        d.direct_execution_id as installation_id,
                        i.measured_item_quantity,
                        d.step
                    FROM direct_execution_item i
                    JOIN direct_execution d ON d.direct_execution_id = i.direct_execution_id
                    WHERE i.contract_item_id = :contractItemId
                        AND d.direct_execution_status <> 'FINISHED'
                    UNION ALL
                    SELECT
                        p.pre_measurement_id as installation_id,
                        i.measured_item_quantity,
                        p.step
                    FROM pre_measurement_street_item i
                    JOIN pre_measurement p ON p.pre_measurement_id = i.pre_measurement_id
                    WHERE i.contract_item_id = :contractItemId
                        AND p.status <> 'FINISHED'
                    ) t
                GROUP BY t.installation_id, t.step
            """.trimIndent()
        } else {
            """
                SELECT
                    iv.installation_id,
                    iv.installation_type,
                    iv.step,
                    SUM(isiv.executed_quantity) AS quantity
                FROM installation_view iv 
                JOIN installation_street_view isv ON iv.installation_id = isv.installation_id
                    and isv.installation_type = iv.installation_type
                JOIN installation_street_item_view isiv ON isiv.installation_street_id = isv.installation_street_id
                    and isv.installation_type = isiv.installation_type
                WHERE isiv.contract_item_id = :contractItemId AND iv.status = 'FINISHED'
                GROUP BY iv.installation_id, iv.installation_type, iv.step
                ORDER BY iv.step
            """.trimIndent()
        }
        val directExecutions = JdbcUtil.getRawData(
            namedJdbc,
            sql,
            mapOf("contractItemId" to contractItemId)
        )

        return directExecutions.mapIndexed { index, row ->
            ExecutedQuantity(
                installationId = row["installation_id"] as Long,
                step = (row["step"] as Number).toInt(), // ✅ usa a etapa real
                quantity = row["quantity"] as BigDecimal,
            )
        }
    }

    fun archiveById(contractId: Long): ResponseEntity<Any> {
        namedJdbc.update(
            "update contract set status = 'ARCHIVED' where contract_id = :contract_id",
            mapOf("contract_id" to contractId)
        )

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

}
