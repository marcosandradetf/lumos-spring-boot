package com.lumos.lumosspring.execution.service

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.contract.repository.ContractItemsQuantitativeRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.contract.service.ContractService
import com.lumos.lumosspring.dto.direct_execution.DirectExecutionDTO
import com.lumos.lumosspring.dto.direct_execution.DirectExecutionDTOResponse
import com.lumos.lumosspring.dto.direct_execution.SendDirectExecutionDto
import com.lumos.lumosspring.dto.indirect_execution.*
import com.lumos.lumosspring.execution.entities.*
import com.lumos.lumosspring.execution.repository.*
import com.lumos.lumosspring.minio.service.MinioService
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.ReservationManagement
import com.lumos.lumosspring.stock.repository.*
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.*
import com.lumos.lumosspring.util.JdbcUtil.existsRaw
import com.lumos.lumosspring.util.JdbcUtil.getRawData
import com.lumos.lumosspring.util.Utils.formatMoney
import com.lumos.lumosspring.util.Utils.replacePlaceholders
import com.lumos.lumosspring.util.Utils.sendHtmlToPuppeteer
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository,
    private val materialStockRepository: MaterialStockRepository,
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
    private val reservationManagementRepository: ReservationManagementRepository,
    private val minioService: MinioService,
    private val jdbcTemplate: JdbcTemplate,
    private val namedJdbc: NamedParameterJdbcTemplate,
    private val contractRepository: ContractRepository,
    private val directExecutionRepository: DirectExecutionRepository,
    private val directExecutionItemRepository: DirectExecutionRepositoryItem,
    private val jdbcGetExecutionRepository: JdbcGetExecutionRepository,
    private val directExecutionRepositoryStreet: DirectExecutionRepositoryStreet,
    private val directExecutionRepositoryStreetItem: DirectExecutionRepositoryStreetItem,
    private val materialStockJdbcRepository: MaterialStockJdbcRepository,
    private val contractService: ContractService,
    private val materialRepository: MaterialRepository,
    private val jdbcInstallationRepository: JdbcInstallationRepository,
    private val directExecutionExecutorRepository: DirectExecutionExecutorRepository,
    private val contractItemsQuantitativeRepository: ContractItemsQuantitativeRepository,
) {

//    // delegar ao estoquista a função de GERENCIAR A RESERVA DE MATERIAIS
//    @Transactional
//    fun delegate(delegateDTO: DelegateDTO): ResponseEntity<Any> {
//        val stockist =
//            userRepository.findByUserId(UUID.fromString(delegateDTO.stockistId))
//                .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body(DefaultResponse("Estoquista não encontrado"))
//
//        val streets = preMeasurementStreetRepository.getAllByPreMeasurement_PreMeasurementIdAndStep(
//            delegateDTO.preMeasurementId,
//            delegateDTO.preMeasurementStep
//        )
//
//        if (streets.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
//            .body(DefaultResponse("Nenhuma rua foi Encontrada"))
//
//        val existingManagement = reservationManagementRepository
//            .existsByStreetsPreMeasurementPreMeasurementIdAndStreetsStep(
//                delegateDTO.preMeasurementId,
//                delegateDTO.preMeasurementStep
//            )
//
//        if (existingManagement) {
//            return ResponseEntity.status(HttpStatus.CONFLICT)
//                .body(DefaultResponse("Já existe uma gestão de reserva para esse estoquista e essas ruas."))
//        }
//
//        val management = ReservationManagement()
//        management.description = delegateDTO.description
//        management.stockist = stockist
//
//        reservationManagementRepository.save(management)
//
//        val currentUserUUID = UUID.fromString(delegateDTO.currentUserUUID)
//        for (delegateStreet in delegateDTO.street) {
//            val team = teamRepository.findById(delegateStreet.teamId)
//                .orElse(null) ?: throw IllegalStateException()
//            val assignBy = userRepository.findByUserId(currentUserUUID)
//                .orElse(null) ?: throw IllegalStateException()
//            val prioritized = delegateStreet.prioritized
//            val comment = delegateStreet.comment
//
//            streets.find { it.preMeasurementStreetId == delegateStreet.preMeasurementStreetId }
//                ?.assignToStockistAndTeam(team, assignBy, util.dateTime, prioritized, comment, management)
//                ?: throw IllegalStateException("A rua ${delegateStreet.preMeasurementStreetId} enviada não foi encontrada")
//
//        }
//
//        preMeasurementStreetRepository.saveAll(streets)
//
//        return ResponseEntity.ok().build()
//    }

    @Transactional
    fun delegateDirectExecution(execution: DirectExecutionDTO): ResponseEntity<Any> {
        val stockist =
            userRepository.findByUserId(execution.stockistId)
                .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(DefaultResponse("Estoquista não encontrado"))

        val contract = contractRepository.findById(execution.contractId)
            .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(DefaultResponse("Contrato não encontrado"))

        val currentUserUUID = execution.currentUserId
//
//        val exists = JdbcUtil.getSingleRow(
//            namedJdbc,
//            """
//                select 1 as result from direct_execution
//                where contract_id = :contractId
//                and direct_execution_status <> :status
//                limit 1
//            """.trimIndent(),
//            mapOf(
//                "contractId" to execution.contractId,
//                "status" to ExecutionStatus.FINISHED
//            )
//        )?.get("result") != null
//
//        if (exists) {
//            throw IllegalArgumentException(
//                """
//                    Existe uma etapa em andamento para este contrato que ainda não foi finalizada.
//                    Para criar uma nova etapa, finalize a etapa atual ou acompanhe o progresso no sistema.
//                """.trimIndent()
//            )
//        }

        val step: Int = namedJdbc.queryForObject(
            """
                    select count(direct_execution_id) + 1 as step from direct_execution
                    where contract_id = :contractId
                """.trimIndent(),
            mapOf("contractId" to execution.contractId),
            Int::class.java
        )!!

        var management = ReservationManagement(
            description = "Etapa $step - ${contract.contractor}",
            stockistId = stockist.userId,
        )
        management = reservationManagementRepository.save(management)

        var directExecution = DirectExecution(
            description = "Etapa $step - ${contract.contractor}",
            instructions = execution.instructions,
            contractId = execution.contractId,
            teamId = execution.teamId,
            assignedBy = currentUserUUID,
            reservationManagementId = management.reservationManagementId
                ?: throw IllegalStateException("Execution Service - reservationManagementId não foi gerado!"),
            assignedAt = Instant.now(),
            step = step
        )

        directExecution = directExecutionRepository.save(directExecution)

        for (item in execution.items) {
            val ciq = contractService.queryContractItems(contract.contractId!!).find { map ->
                val contractItemId = (map["contract_Item_Id"] as? Number)?.toLong()
                contractItemId == item.contractItemId
            }

            val description = JdbcUtil.getSingleRow(
                namedJdbc,
                """
                    select name_for_import from contract_reference_item
                    where contract_reference_item_id = :referenceId
                """.trimIndent(),
                mapOf("referenceId" to (ciq?.get("contract_reference_item_id") ?: -1) as Long)
            )


            if (ciq != null) {
                val contractedQuantity = BigDecimal((ciq["contracted_quantity"] as Number).toString())
                val executedQuantity = BigDecimal((ciq["quantity_executed"] as Number).toString())

                var available = contractedQuantity - executedQuantity

                if (available < item.quantity) {
                    throw IllegalStateException("Não há saldo disponível para o item $description")
                }

                available = contractItemsQuantitativeRepository.getTotalBalance(item.contractItemId)

                if (available < item.quantity) {
//                    val response = contractItemsQuantitativeRepository.getInProgressReservations(item.contractItemId)
//                    if (response.isEmpty())
                    throw Utils.BusinessException("Apesar de no sistema existir saldo para o item $description existem execuções em andamentos que podem fazer o saldo estourar")

//                    throw Utils.BusinessExceptionObjectResponse(response)
                }

                val directExecutionItem = DirectExecutionItem(
                    measuredItemQuantity = item.quantity,
                    contractItemId = item.contractItemId,
                    itemStatus = if (listOf(
                            "SERVIÇO",
                            "PROJETO"
                        ).contains(ciq["type"] as String)
                    ) ReservationStatus.FINISHED else ReservationStatus.PENDING,
                    directExecutionId = directExecution.directExecutionId
                        ?: throw IllegalStateException("Id da execução não encontrado"),

                    )
                directExecutionItemRepository.save(directExecutionItem)
            }
        }

        notificationService.sendNotificationForUserId(
            title = "Nova Ordem Disponível",
            body = "Uma nova ordem foi atribuída a você. Acesse a tela de Gerenciamento de Reservas para iniciar o processo.",
            userId = execution.stockistId.toString(),
            time = Instant.now(),
            type = NotificationType.ALERT,
        )

        return ResponseEntity.ok().body(DefaultResponse("$step etapa criada com sucesso"))
    }

    fun getPendingReservesForStockist(userUUID: UUID): ResponseEntity<Any> {
        val response = mutableListOf<ReserveDTOResponse>()
        val pendingManagement = getRawData(
            namedJdbc,
            "select rm.reservation_management_id, rm.description from reservation_management rm \n" +
                    "where rm.status = :status and rm.stockist_id = :stockist_id",
            mapOf("status" to ReservationStatus.PENDING, "stockist_id" to userUUID)
        )

        for (pRow in pendingManagement) {
            val reservationManagementId = (pRow["reservation_management_id"] as? Number)?.toLong() ?: 0L
            val description = pRow["description"] as? String ?: ""
            val directExecutionWaiting = getRawData(
                namedJdbc,
                "select de.direct_execution_id, au.name || ' ' || au.last_name as completedName ,  t.id_team, t.team_name, d.deposit_name from direct_execution de \n" +
                        "inner join team t on t.id_team = de.team_id \n" +
                        "inner join deposit d on d.id_deposit = t.deposit_id_deposit \n" +
                        "inner join app_user au on au.user_id = de.assigned_user_id \n" +
                        "where de.reservation_management_id = :reservation_management_id \n",
                mapOf("reservation_management_id" to reservationManagementId)
            )

            val directExecutions: List<ReserveStreetDTOResponse> = directExecutionWaiting.map { row ->
                val directExecutionId = (row["direct_execution_id"] as? Number)?.toLong() ?: 0L

                val items = namedJdbc.query(
                    """
                            SELECT dei.direct_execution_item_id as itemId, cri.name_for_import as description,
                            dei.measured_item_quantity as quantity, cri.type, cri.linking
                            FROM direct_execution_item dei
                            INNER JOIN contract_item ci ON ci.contract_item_id = dei.contract_item_id
                            INNER JOIN contract_reference_item cri ON cri.contract_reference_item_id = ci.contract_item_reference_id
                            WHERE dei.direct_execution_id = :direct_execution_id
                                AND dei.item_status = :itemStatus AND cri.type NOT IN ('SERVIÇO', 'PROJETO', 'MANUTENÇÃO','EXTENSÃO DE REDE', 'TERCEIROS')
                            ORDER BY cri.name_for_import
                        """.trimIndent(),
                    MapSqlParameterSource(
                        mapOf(
                            "direct_execution_id" to directExecutionId,
                            "itemStatus" to ReservationStatus.PENDING
                        )
                    ),
                ) { rs, _ ->
                    ItemResponseDTO(
                        itemId = rs.getLong("itemId"),
                        description = rs.getString("description"),
                        quantity = rs.getBigDecimal("quantity"),
                        type = rs.getString("type"),
                        linking = rs.getString("linking")
                    )
                }

                ReserveStreetDTOResponse(
                    preMeasurementStreetId = null,
                    directExecutionId = directExecutionId,
                    streetName = "",
                    latitude = null,
                    longitude = null,
                    prioritized = false,
                    comment = "DIRECT_EXECUTION",
                    assignedBy = row["completedName"]?.toString() ?: "",
                    teamId = (row["id_team"] as? Number)?.toLong() ?: 0L,
                    teamName = row["team_name"]?.toString() ?: "",
                    truckDepositName = row["deposit_name"]?.toString() ?: "",
                    items = items
                )
            }

            val inDirectExecutionWaiting = getRawData(
                namedJdbc,
                "select pms.pre_measurement_street_id, pms.street, pms.latitude, pms.longitude, pms.prioritized, rm.description, \n" +
                        "pms.comment, au.name || ' ' || au.last_name as completedName , t.id_team, t.team_name, d.deposit_name \n" +
                        "from pre_measurement_street pms \n" +
                        "inner join team t on t.id_team = pms.team_id \n" +
                        "inner join deposit d on d.id_deposit = t.deposit_id_deposit \n" +
                        "inner join app_user au on au.user_id = pms.assigned_by_user_id \n" +
                        "inner join reservation_management rm on rm.reservation_management_id = pms.reservation_management_id \n" +
                        "where pms.reservation_management_id = :reservation_management_id",
                mapOf("reservation_management_id" to reservationManagementId)
            )

            val indirectExecutions: List<ReserveStreetDTOResponse> = inDirectExecutionWaiting.map { row ->
                val preMeasurementStreetId = (row["pre_measurement_street_id"] as? Number)?.toLong() ?: 0L

                val items = namedJdbc.query(
                    """
                            SELECT pmsi.pre_measurement_street_item_id as itemId, cri.name_for_import as description,
                            pmsi.measured_item_quantity as quantity, cri.type, cri.linking
                            FROM pre_measurement_street_item pmsi
                            INNER JOIN contract_item ci ON ci.contract_item_id = pmsi.contract_item_id
                            INNER JOIN contract_reference_item cri ON cri.contract_reference_item_id = ci.contract_item_reference_id
                            WHERE pmsi.pre_measurement_street_id = :pre_measurement_street_id and pmsi.item_status = :itemStatus
                            AND cri.type NOT IN ('SERVIÇO', 'PROJETO', 'MANUTENÇÃO','EXTENSÃO DE REDE', 'TERCEIROS')
                        """.trimIndent(),
                    MapSqlParameterSource(
                        mapOf(
                            "pre_measurement_street_id" to preMeasurementStreetId,
                            "itemStatus" to ReservationStatus.PENDING
                        )
                    )
                ) { rs, _ ->
                    ItemResponseDTO(
                        itemId = rs.getLong("itemId"),
                        description = rs.getString("description"),
                        quantity = rs.getBigDecimal("quantity"),
                        type = rs.getString("type"),
                        linking = rs.getString("linking")
                    )
                }

                ReserveStreetDTOResponse(
                    preMeasurementStreetId = preMeasurementStreetId,
                    directExecutionId = null,
                    streetName = row["street"]?.toString() ?: "",
                    latitude = (row["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (row["longitude"] as? Number)?.toDouble() ?: 0.0,
                    prioritized = (row["prioritized"] as? Boolean) ?: false,
                    comment = (row["comment"] as? String) ?: "",
                    assignedBy = (row["completedName"] as? String) ?: "",
                    teamId = (row["id_team"] as? Number)?.toLong() ?: 0L,
                    teamName = row["team_name"]?.toString() ?: "",
                    truckDepositName = (row["deposit_name"] as? String) ?: "",
                    items = items
                )
            }

            val executions = directExecutions.ifEmpty { indirectExecutions }
            response.add(ReserveDTOResponse(description, executions))
        }

        return ResponseEntity.ok(response)
    }

    fun getStockMaterialForLinking(linking: String, type: String, teamId: Long): ResponseEntity<Any> {
        val materials: List<MaterialInStockDTO> = if (type != "NULL" && linking != "NULL") {
            materialStockJdbcRepository.findAllByLinkingAndType(
                linking.lowercase(),
                type.lowercase(),
                teamId
            )
        } else {
            materialStockJdbcRepository.findAllByType(type.lowercase(), teamId)
        }

        return ResponseEntity.ok(materials)
    }

    @Transactional
    fun reserveMaterialsForExecution(executionReserve: ReserveDTOCreate, strUserUUID: String): ResponseEntity<Any> {
        val preMeasurementStreetId = executionReserve.preMeasurementStreetId
        val preMeasurementStreet = if (preMeasurementStreetId != null) {
            preMeasurementStreetRepository.findById(preMeasurementStreetId)
                .orElseThrow { IllegalStateException("Street not found for id: $preMeasurementStreetId") }
        } else null

        if (preMeasurementStreet != null)
            if (preMeasurementStreet.streetStatus != ExecutionStatus.WAITING_STOCKIST) {
                return ResponseEntity.status(500)
                    .body(DefaultResponse("Os itens dessa execução já foram todos reservados, inicie a próxima etapa."))
            }

        val directExecution = executionReserve.directExecutionId?.let {
            directExecutionRepository.findById(it)
                .orElseThrow { IllegalArgumentException("Execução não encontrada: $it") }
        }

        val reservations = mutableListOf<MaterialReservation>()
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            IllegalArgumentException(e.message)
        }

        val depositId = JdbcUtil.getSingleRow(
            namedJdbc,
            """
                SELECT deposit_id_deposit from team
                where id_team = :teamId
            """.trimIndent(),
            mapOf("teamId" to executionReserve.teamId)
        )?.get("deposit_id_deposit") as? Long ?: throw IllegalStateException("Deposit Id not found")

        for (item in executionReserve.items) {
            for (materialReserve in item.materials) {

                val contractItemId =
                    if (preMeasurementStreetId != null)
                        JdbcUtil.getSingleRow(
                            namedJdbc,
                            """
                                select contract_item_id from pre_measurement_street_item
                                where pre_measurement_street_item_id = :streetItemId
                            """.trimIndent(),
                            mapOf("streetItemId" to item.itemId)
                        )?.get("contract_item_id") as? Long
                            ?: throw IllegalStateException("Contrato do item ${item.itemId} enviado não foi encontrado")
                    else
                        JdbcUtil.getSingleRow(
                            namedJdbc,
                            """
                                select contract_item_id from direct_execution_item
                                where direct_execution_item_id = :directItemId
                            """.trimIndent(),
                            mapOf("directItemId" to item.itemId)
                        )?.get("contract_item_id") as? Long
                            ?: throw IllegalStateException("Contrato do item ${item.itemId} enviado não foi encontrado")

                val available = contractItemsQuantitativeRepository.getTotalBalance(contractItemId)

                if (available < materialReserve.materialQuantity) {
                    val response = contractItemsQuantitativeRepository.getInProgressReservations(contractItemId)
                    if (response.isEmpty()) throw Utils.BusinessException("Apesar de no sistema existir saldo para o item $contractItemId existem execuções em andamentos que podem fazer o saldo estourar")
                    var message = """
                        O sistema mostra saldo disponível para o item ${response[0].itemName}, porém existem reservas para instalações em andamento que podem causar estouro no saldo.
                        
                        Instalações afetadas:
                        """
                    for (r in response) {
                        message += """          
                            ${r.description}
                                - Quantidade Reservada: ${r.reservedQuantity}
                                - Quantidade Completada: ${r.quantityCompleted}
                            ___________________
                        """
                    }

                    message += """
                        Saldo atual: ${contractItemsQuantitativeRepository.getTotalBalance(contractItemId)}
                    """

                    throw Utils.BusinessException(message)
                }

                val reservation = if (materialReserve.centralMaterialStockId != null) {
                    val materialStock =
                        materialStockRepository.findById(materialReserve.centralMaterialStockId) // conferir
                            .orElse(null) ?: throw IllegalStateException("Material não encontrado")

                    val material = materialRepository.findById(materialStock.materialId).orElseThrow()

                    if (materialStock.stockAvailable < materialReserve.materialQuantity)
                        throw IllegalArgumentException("O material ${material.materialName} não possuí estoque suficiente")

                    val stockistMatch = existsRaw(
                        namedJdbc,
                        """
                                select 1 from material_stock ms
                                inner join stockist s on s.deposit_id_deposit = ms.deposit_id
                                where ms.material_id_stock = :materialId and s.user_id_user = :userId
                                limit 1
                            """.trimIndent(),
                        mapOf("materialId" to materialReserve.centralMaterialStockId, "userId" to userUUID)
                    )

                    val status = if (stockistMatch) {
                        namedJdbc.update(
                            """
                            UPDATE material_stock set stock_available = stock_available - :materialQuantity
                            WHERE material_id_stock = :centralMaterialStockId
                        """.trimIndent(),
                            mapOf(
                                "materialQuantity" to materialReserve.materialQuantity,
                                "centralMaterialStockId" to materialReserve.centralMaterialStockId
                            )
                        )

                        ReservationStatus.APPROVED
                    } else ReservationStatus.PENDING

                    val truckMaterialStockId = JdbcUtil.getSingleRow(
                        namedJdbc,
                        """
                            select material_id_stock from material_stock
                            where material_id = :materialId
                            and deposit_id = :depositId
                        """.trimIndent(),
                        mapOf("materialId" to materialReserve.materialId, "depositId" to depositId)
                    )?.get("material_id_stock") as? Long
                        ?: throw IllegalStateException("Truck Material stock not found ${materialReserve.materialId} - deposit: $depositId")

                    MaterialReservation(
                        description = preMeasurementStreet?.street,
                        reservedQuantity = materialReserve.materialQuantity,
                        preMeasurementStreetId = executionReserve.preMeasurementStreetId,
                        directExecutionId = executionReserve.directExecutionId,
                        contractItemId = contractItemId,
                        teamId = executionReserve.teamId,
                        centralMaterialStockId = materialReserve.centralMaterialStockId,
                        truckMaterialStockId = truckMaterialStockId,
                        status = status
                    )

                } else {
                    MaterialReservation(
                        description = preMeasurementStreet?.street,
                        reservedQuantity = materialReserve.materialQuantity,
                        preMeasurementStreetId = executionReserve.preMeasurementStreetId,
                        directExecutionId = executionReserve.directExecutionId,
                        contractItemId = contractItemId,
                        teamId = executionReserve.teamId,
                        truckMaterialStockId = materialReserve.truckMaterialStockId
                            ?: throw IllegalStateException("Truck Material stock Id not found"),
                        status = ReservationStatus.IN_STOCK
                    )
                }

                reservations.add(reservation)
            }
        }

        materialReservationRepository.saveAll(reservations)

        val responseMessage =
            if (preMeasurementStreet != null) verifyStreetReservations(reservations, preMeasurementStreet)
            else if (directExecution != null) verifyDirectExecutionReservations(reservations, directExecution) else ""

        return ResponseEntity.ok().body(DefaultResponse(responseMessage))
    }

    private fun verifyStreetReservations(
        reservation: List<MaterialReservation>,
        preMeasurementStreet: PreMeasurementStreet
    ): String {
        var responseMessage = ""
        if (reservation.all { it.status == ReservationStatus.IN_STOCK }) {
            preMeasurementStreet.streetStatus = ExecutionStatus.AVAILABLE_EXECUTION
            responseMessage =
                "Como todos os itens estão no caminhão, nenhuma ação adicional será necessária. A equipe pode iniciar a execução."

        } else if (!reservation.any { it.status == ReservationStatus.PENDING }) {
            preMeasurementStreet.streetStatus = ExecutionStatus.WAITING_COLLECT
            responseMessage =
                "Como nenhum material foi reservado em almoxarifado de terceiros. Não será necessário aprovação. " +
                        "Mas os materiais estão pendentes de coleta pela equipe."

        } else if (reservation.any { it.status == ReservationStatus.PENDING }) {
            preMeasurementStreet.streetStatus = ExecutionStatus.WAITING_RESERVE_CONFIRMATION

            responseMessage =
                "Como alguns itens foram reservadas em almoxarifados de terceiros. Será necessária aprovação. " +
                        "Após isso estes materiais estarão disponíveis para coleta."
        }

        notify(reservation, preMeasurementStreet.preMeasurementStreetId.toString())

        preMeasurementStreetRepository.save(preMeasurementStreet)

        TODO("CORRIGIR")
//        if (
//            preMeasurementStreet.reservationManagement.streets
//                .none { it.streetStatus == ExecutionStatus.WAITING_STOCKIST }
//        ) {
//            preMeasurementStreet.reservationManagement.status = ReservationStatus.FINISHED
//            namedJdbc.update(
//                """
//                    update pre_measurement_street_item set item_status = :status
//                    where pre_measurement_street_id = :preMeasurementStreetId
//                """.trimIndent(),
//                mapOf(
//                    "status" to ReservationStatus.FINISHED,
//                    "preMeasurementStreetId" to preMeasurementStreet.preMeasurementStreetId
//                )
//            )
//        }

        preMeasurementStreetRepository.save(preMeasurementStreet)

        return responseMessage
    }

    private fun verifyDirectExecutionReservations(
        reservation: List<MaterialReservation>,
        directExecution: DirectExecution
    ): String {
        var responseMessage = ""
        if (reservation.all { it.status == ReservationStatus.IN_STOCK }) {
            directExecution.directExecutionStatus = ExecutionStatus.AVAILABLE_EXECUTION
            responseMessage =
                "Como todos os itens estão no caminhão, nenhuma ação adicional será necessária. A equipe pode iniciar a execução."

        } else if (!reservation.any { it.status == ReservationStatus.PENDING }) {
            directExecution.directExecutionStatus = ExecutionStatus.WAITING_COLLECT
            responseMessage =
                "Como nenhum material foi solicitado em almoxarifado de terceiros. Não será necessário aprovação. " +
                        "Mas os materiais estão pendentes de coleta pela equipe."

        } else if (reservation.any { it.status == ReservationStatus.PENDING }) {
            directExecution.directExecutionStatus = ExecutionStatus.WAITING_RESERVE_CONFIRMATION
            responseMessage =
                "Como alguns itens foram solicitados em almoxarifados de terceiros. Será necessária aprovação. " +
                        "Após isso estes materiais estarão disponíveis para coleta."
        }

        notify(reservation, directExecution.directExecutionId.toString())

        jdbcTemplate.update(
            """
            UPDATE reservation_management set status = ?
            where reservation_management_id = ?
        """.trimIndent(),
            ReservationStatus.FINISHED, directExecution.reservationManagementId
        )

        namedJdbc.update(
            """
                    update direct_execution_item set item_status = :status
                    where direct_execution_id = :directExecutionId
                """.trimIndent(),
            mapOf(
                "status" to ReservationStatus.FINISHED,
                "directExecutionId" to directExecution.directExecutionId
            )
        )

        directExecutionRepository.save(directExecution)

        return responseMessage
    }

    private fun notify(reservations: List<MaterialReservation>, streetIdOrExecutionId: String) {
        val reservationIds = reservations.map { it.materialIdReservation }
        val statuses = listOf(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        var bodyMessage: String
        val companyPhone = JdbcUtil.getDescription(
            jdbcTemplate,
            field = "company_phone",
            table = "company",
            type = String::class.java,
        )

        val pendingReserves = getRawData(
            namedJdbc,
            "select mr.status, ms.deposit_id, t.id_team from material_reservation mr \n" +
                    "inner join material_stock ms on ms.material_id_stock = mr.central_material_stock_id \n" +
                    "inner join deposit d on d.id_deposit = ms.deposit_id \n" +
                    "inner join team t on t.id_team = mr.team_id \n" +
                    "where mr.material_id_reservation in (:material_id_reservations) and mr.status in (:statuses)",
            mapOf("material_id_reservations" to reservationIds, "statuses" to statuses)
        )

        val groupedByDepositPending = pendingReserves
            .filter { row -> (row["status"] as? String) == ReservationStatus.PENDING }
            .groupBy { row -> (row["deposit_id"] as? Number)?.toLong() }

        val groupedByDepositApproved = pendingReserves
            .filter { row -> (row["status"] as? String) == ReservationStatus.APPROVED }
            .groupBy { row -> (row["deposit_id"] as? Number)?.toLong() }


        // Itera sobre os grupos e envia notificação para equipe por depósito
        groupedByDepositPending.forEach { (depositId, reserve) ->
            val deposit = depositRepository.findById(depositId ?: 0).orElse(null)
            val teamId = (reserve.first()["id_team"] as? Number)?.toLong()
            val team = teamRepository.findById(teamId ?: 0).orElse(null)

//            val teamCode = team?.teamCode

            bodyMessage = """
                Por favor, aceite ou negue as solicitações com urgência!
                """.trimIndent()

//            if (teamCode != null) {
//                notificationService.sendNotificationForTeam(
//                    team = teamCode,
//                    title = "Existem ${reserve.size} materiais pendentes de aprovação no seu almoxarifado (${deposit?.depositName ?: ""})",
//                    body = bodyMessage,
//                    action = "REPLY_RESERVE",
//                    time = Instant.now(),
//                    type = NotificationType.ALERT,
//                    persistCode = streetIdOrExecutionId
//                )
//            }

        }

        // Itera sobre os grupos e envia notificação para equipe por depósito
        groupedByDepositApproved.forEach { (depositId, reserve) ->
            val deposit = depositRepository.findById(depositId ?: 0).orElse(null)
            val teamId = (reserve.first()["id_team"] as? Number)?.toLong()

            val stockistUUID = JdbcUtil.getSingleRow(
                namedJdbc,
                """
                    SELECT user_id_user
                    FROM stockist
                    WHERE deposit_id_deposit = :depositId
                    LIMIT 1
                """.trimIndent(),
                mapOf("depositId" to depositId)
            )?.get("user_id_user") as? String

            val team = teamRepository.findById(teamId ?: 0).orElse(null)

//            val teamCode = team?.teamCode ?: ""
            val quantity = reserve.size
            val stockistName = JdbcUtil.getDescription(
                jdbcTemplate,
                field = "name || ' ' || last_name",
                table = "app_user",
                where = "user_id",
                equal = stockistUUID ?: UUID(0L, 0L),
                type = String::class.java,
            )

            val depositName = deposit?.depositName ?: "Desconhecido"
            val address = deposit?.depositAddress ?: "Endereço não informado"
            val phone = deposit?.depositPhone ?: companyPhone ?: "Telefone não Informado"
            val responsible = stockistName ?: "Responsável não informado"

            bodyMessage = """
                    Local: $depositName"
                    Endereço: $address"
                    Telefone: $phone"
                    Responsável: $responsible"
                """.trimIndent()

//            notificationService.sendNotificationForTeam(
//                team = teamCode,
//                title = "Existem $quantity materiais pendentes de coleta no almoxarifado ${deposit?.depositName ?: ""}",
//                body = bodyMessage,
//                action = "",
//                time = Instant.now(),
//                type = NotificationType.ALERT,
//                persistCode = streetIdOrExecutionId
//            )

        }

    }

    fun getReservationsByStatusAndStockist(depositId: Long, status: String): ResponseEntity<Any> {
        data class ReservationDto(
            val reserveId: Long?,
            val materialId: Long?,
            val orderId: String?,

            val reserveQuantity: Double?,
            val stockQuantity: Double,
            val materialName: String,
            val description: String?,
            val status: String,
        )

        data class ReservationsByCaseDtoResponse(
            val description: String,
            val teamName: String?,
            val reservations: List<ReservationDto>
        )

        val response: MutableList<ReservationsByCaseDtoResponse> = mutableListOf()

        val rawReservations = getRawData(
            namedJdbc,
            """
                    -- Reservas para execucoes
                    select pms.city, c.contractor, mr.material_id_reservation, cast(null as uuid) as order_id,
                    mr.reserved_quantity as request_quantity, mr.description, m.id_material,
                    m.material_name, m.material_power, m.material_length, t.team_name,
                    ms.stock_quantity, mr.status, cast(null as timestamp) as created_at
                    from material_reservation mr
                    inner join material_stock ms on ms.material_id_stock = mr.central_material_stock_id
                    inner join material m on m.id_material = ms.material_id
                    left join direct_execution de on mr.direct_execution_id = de.direct_execution_id
                    left join pre_measurement_street pms on pms.pre_measurement_street_id = mr.pre_measurement_street_id
                    inner join team t on t.id_team = mr.team_id
                    left join contract c on de.contract_id = c.contract_id
                    where ms.deposit_id = :deposit_id and mr.status = :status

                    UNION ALL

                    -- Pedidos da equipe
                    select cast(null as text) as city, cast(null as text) as contractor, cast(null as bigint) as material_id_reservation, om.order_id,
                    cast(null as bigint) as request_quantity, om.order_code as description, m.id_material,
                    m.material_name, m.material_power, m.material_length, t.team_name,
                    ms.stock_quantity, om.status, om.created_at
                    from order_material om
                    inner join order_material_item omi on omi.order_id = om.order_id
                    inner join material_stock ms on ms.material_id = omi.material_id
                    inner join material m on m.id_material = ms.material_id
                    inner join team t on t.id_team = om.team_id
                    where ms.deposit_id = :deposit_id and om.status = :status and ms.deposit_id = om.deposit_id

                    order by created_at nulls last, material_id_reservation nulls last;
                    """.trimIndent(),
            mapOf("deposit_id" to depositId, "status" to status)
        )

        val reservationsGroup = rawReservations
            .groupBy {
                (it["city"] as? String) ?: (it["contractor"] as? String) ?: (it["description"] as? String)
                ?: "Desconhecido"
            }

        for ((preMeasurementName, reservations) in reservationsGroup) {
            val list = mutableListOf<ReservationDto>()
            for (reserve in reservations) {
                var materialName = (reserve["material_name"] as String)
                val power: String? = (reserve["material_power"] as? String)
                val length: String? = (reserve["material_length"] as? String)
                if (power != null) materialName += " $power"
                else if (length != null) materialName += " $length"

                list.add(
                    ReservationDto(
                        reserveId = (reserve["material_id_reservation"] as Number?)?.toLong(),
                        orderId = (reserve["order_id"] as? UUID)?.toString(),

                        reserveQuantity = (reserve["request_quantity"] as? Number)?.toDouble(),
                        stockQuantity = (reserve["stock_quantity"] as Number).toDouble(),
                        materialId = reserve["id_material"] as Long,
                        materialName = materialName,
                        description = (reserve["description"] as? String),
                        status = reserve["status"] as String,
                    )
                )
            }

            response.add(
                ReservationsByCaseDtoResponse(
                    description = preMeasurementName,
                    teamName = reservations.first()["team_name"] as? String,
                    reservations = list
                )
            )
        }


        return ResponseEntity.ok().body(response)
    }

    @Transactional
    fun uploadIndirectExecution(photo: MultipartFile, executionDTO: SendExecutionDto?): ResponseEntity<Any> {
        if (executionDTO == null) {
            return ResponseEntity.badRequest().body("Execution DTO está vazio.")
        }

        val execution = JdbcUtil.getSingleRow(
            namedJdbc,
            """
                    SELECT street_status, city FROM pre_measurement_street
                    WHERE pre_measurement_street_id = :streetId
                """.trimIndent(),
            mapOf("streetId" to executionDTO.streetId)
        )
        if (execution == null) throw Utils.BusinessException("Street com ID ${executionDTO.streetId} não encontrada")

        if (execution["street_status"] as String != ExecutionStatus.AVAILABLE_EXECUTION) {
            return ResponseEntity.badRequest().body("Execução já enviada")
        }

        val city: String? = execution["city"] as String?
        val folder = if (city == null) "photos"
        else "photos/$city"

        val fileUri = minioService.uploadFile(photo, "scl-construtora", folder, "execution")

        var sql = """
            UPDATE pre_measurement_street
            SET execution_photo_uri = :fileUri,
            street_status = :streetStatus
            WHERE pre_measurement_street_id = :streetId
        """.trimIndent()
        namedJdbc.update(
            sql, mapOf(
                "fileUri" to fileUri,
                "streetStatus" to ExecutionStatus.FINISHED,
                "streetId" to executionDTO.streetId
            )
        )

        for (r in executionDTO.reserves) {
            val exists = existsRaw(
                namedJdbc,
                """
                    SELECT 1 FROM material_reservation
                    WHERE material_id_reservation = :reserveId
                """.trimIndent(),
                mapOf("reserveId" to r.reserveId)
            )

            if (!exists) {
                throw Utils.BusinessException("Reserva com ID ${r.reserveId} não encontrada")
            }


            sql = if (r.materialName.contains("led", ignoreCase = true)) {
                """
                    UPDATE contract_item ci
                    SET quantity_executed = quantity_executed + :quantityExecuted
                    FROM pre_measurement_street_item pmsi, contract_reference_item cri
                    WHERE (cri.item_dependency = 'LED' OR ci.contract_item_id = :contractItemId)
                        AND cri.contract_reference_item_id = ci.contract_item_reference_id
                        AND pmsi.contract_item_id = ci.contract_item_id
                """.trimIndent()
            } else if (r.materialName.contains("braço", ignoreCase = true)) {
                """
                    UPDATE contract_item ci
                    SET quantity_executed = quantity_executed + :quantityExecuted
                    FROM pre_measurement_street_item pmsi, contract_reference_item cri
                    WHERE (cri.item_dependency = 'BRAÇO' OR ci.contract_item_id = :contractItemId)
                        AND cri.contract_reference_item_id = ci.contract_item_reference_id
                        AND pmsi.contract_item_id = ci.contract_item_id
                """.trimIndent()
            } else {
                """
                    UPDATE contract_item
                    SET quantity_executed = quantity_executed + :quantityExecuted
                    WHERE contract_item_id = :contractItemId
                """.trimIndent()
            }

            namedJdbc.update(
                sql,
                mapOf(
                    "quantityExecuted" to r.quantityExecuted,
                    "contractItemId" to r.contractItemId
                ),
            )

            val remaining = namedJdbc.queryForObject(
                """
                    UPDATE material_reservation
                    SET status = :status, quantity_completed = :quantityCompleted
                    WHERE material_id_reservation = :reserveId
                    RETURNING  reserved_quantity - quantity_completed  AS remaining_quantity
                """.trimIndent(),
                mapOf(
                    "status" to ReservationStatus.FINISHED,
                    "quantityCompleted" to r.quantityExecuted,
                    "reserveId" to r.reserveId,
                ),
            ) { rs, _ -> rs.getDouble("remaining_quantity") }

            if (remaining!! > 0.1)
                namedJdbc.update(
                    """
                        UPDATE material_stock
                        SET stock_available = stock_available + :remaining,
                            stock_quantity = stock_quantity + :remaining
                        WHERE material_id_stock = :truckMaterialIdStock
                    """.trimIndent(),
                    mapOf(
                        "remaining" to remaining,
                        "truckMaterialIdStock" to r.truckMaterialStockId
                    )
                )
        }

        return ResponseEntity.ok().build()
    }

    @Transactional
    fun uploadDirectExecution(photo: MultipartFile, executionDTO: SendDirectExecutionDto?): ResponseEntity<Any> {
        if (executionDTO == null) {
            return ResponseEntity.badRequest().body("Execution DTO está vazio.")
        }

        val exists = JdbcUtil.getSingleRow(
            namedJdbc,
            """
                SELECT 1 as result
                FROM direct_execution de
                JOIN direct_execution_street des on des.direct_execution_id = de.direct_execution_id
                WHERE des.device_street_id = :deviceStreetId AND des.device_id = :deviceId and de.direct_execution_status = 'FINISHED'
            """.trimIndent(),
            mapOf("deviceStreetId" to executionDTO.deviceStreetId, "deviceId" to executionDTO.deviceId)
        )?.get("result") != null

        if (exists) {
            return ResponseEntity.status(409).body("Rua já enviada antes ou execução já finalizada!")
        }

        var executionStreet = DirectExecutionStreet(
            lastPower = executionDTO.lastPower,
            address = executionDTO.address,
            latitude = executionDTO.latitude,
            longitude = executionDTO.longitude,
            deviceStreetId = executionDTO.deviceStreetId,
            deviceId = executionDTO.deviceId,
            finishedAt = executionDTO.finishAt,
            directExecutionId = executionDTO.directExecutionId,
            currentSupply = executionDTO.currentSupply
        )

        val folder = "photos/${executionDTO.description.replace("\\s+".toRegex(), "_")}"
        val fileUri = minioService.uploadFile(photo, "scl-construtora", folder, "execution")
        executionStreet.executionPhotoUri = fileUri

        try {
            executionStreet = directExecutionRepositoryStreet.save(executionStreet)
        } catch (ex: DataIntegrityViolationException) {
            val rootMessage = ex.mostSpecificCause.message ?: ""
            if (rootMessage.contains("UNIQUE_SEND_STREET", ignoreCase = true)) {
                return ResponseEntity.ok().build()
            }
            throw ex
        }

        for (m in executionDTO.materials) {

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
                directExecutionStreetId = executionStreet.directExecutionStreetId
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
                params["directExecutionId"] = executionDTO.directExecutionId
            }

            if (hasService != null) {
                val servicesData: List<Map<String, Any>> =
                    getRawData(
                        namedJdbc,
                        """
                            WITH to_update AS (
                                SELECT ci.contract_item_id, false as isService
                                FROM contract_item ci
                                WHERE ci.contract_item_id = :contractItemId

                                UNION ALL

                                SELECT ci.contract_item_id, true as isService
                                FROM contract_item ci
                                JOIN contract_reference_item cri ON cri.contract_reference_item_id = ci.contract_item_reference_id
                                JOIN direct_execution_item di ON di.contract_item_id = ci.contract_item_id
                                WHERE lower(cri.item_dependency) = :dependency
                                  AND lower(cri.type) IN ('projeto', 'serviço')
                                  AND di.direct_execution_id = :directExecutionId
                            )
                            UPDATE contract_item ci
                            SET quantity_executed = quantity_executed + :quantityExecuted
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
                        directExecutionStreetId = executionStreet.directExecutionStreetId
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

    fun getIndirectExecutions(strUUID: String?): ResponseEntity<List<IndirectExecutionDTOResponse>> {
        val userUUID = try {
            UUID.fromString(strUUID)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Usuário não encontrado")
        }

        val executions = jdbcGetExecutionRepository.getIndirectExecutions(userUUID)

        return ResponseEntity.ok().body(executions)
    }

    fun getDirectExecutions(strUUID: String?): ResponseEntity<List<DirectExecutionDTOResponse>> {
        val userUUID = try {
            UUID.fromString(strUUID)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Usuário não encontrado")
        }

        val executions = jdbcGetExecutionRepository.getDirectExecutions(userUUID)

        return ResponseEntity.ok().body(executions)
    }

    @Transactional
    fun finishDirectExecution(directExecutionId: Long, operationalUsers: List<UUID>? = null): ResponseEntity<Any> {
        val hasFinished = JdbcUtil.getSingleRow(
            namedJdbc,
            "SELECT 1 as result FROM direct_execution des WHERE direct_execution_status = :status AND direct_execution_id = :directExecutionId",
            mapOf(
                "directExecutionId" to directExecutionId,
                "status" to ExecutionStatus.FINISHED
            ),
        )?.get("result") != null

        if (hasFinished) {
            return ResponseEntity.status(409).body("Execução já finalizada anteriormente")
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

        namedJdbc.update(
            """
            UPDATE direct_execution set direct_execution_status = :status
            where direct_execution_id = :directExecutionId
        """.trimIndent(),
            mapOf(
                "directExecutionId" to directExecutionId,
                "status" to ExecutionStatus.FINISHED
            ),
        )

        return ResponseEntity.ok().build()
    }

    fun getGroupedInstallations(): List<Map<String, JsonNode>> {
        return jdbcInstallationRepository.getGroupedInstallations()
    }

    fun generateDataReport(executionId: Long): ResponseEntity<ByteArray> {
        var templateHtml = this::class.java.getResource("/templates/installation/data.html")!!.readText()

        val data = jdbcInstallationRepository.getDataForReport(executionId)
        val jsonData = data.first() // Pega o único resultado

        val company = jsonData["company"]!!
        val contract = jsonData["contract"]!!
        val values = jsonData["values"]!!
        val columns = jsonData["columns"]!!
        val streets = jsonData["streets"]!!
        val streetSums = jsonData["street_sums"]!!
        val total = jsonData["total"]!!

        val companyBucket =
            company["bucket"]?.asText() ?: throw IllegalArgumentException("Company bucket does not exist")
        val logoUri = company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo does not exist")
        val companyLogoUrl = minioService.getPresignedObjectUrl(companyBucket, logoUri)

        val replacements = mapOf(
            "TITLE" to "RELATÓRIO DE INSTALAÇÃO DE LEDS - " + contract["contract_number"].asText(),
            "CONTRACT_NUMBER" to contract["contract_number"].asText(),
            "COMPANY_SOCIAL_REASON" to company["social_reason"].asText(),
            "COMPANY_CNPJ" to company["company_cnpj"].asText(),
            "COMPANY_ADDRESS" to company["company_address"].asText(),
            "COMPANY_PHONE" to company["company_phone"].asText(),
            "CONTRACTOR_SOCIAL_REASON" to contract["contractor"].asText(),
            "CONTRACTOR_CNPJ" to contract["cnpj"].asText(),
            "CONTRACTOR_ADDRESS" to contract["address"].asText(),
            "CONTRACTOR_PHONE" to contract["phone"].asText(),
            "LOGO_IMAGE" to companyLogoUrl,
            "TOTAL_VALUE" to formatMoney(total["total_price"].asDouble()),
        )

        templateHtml = templateHtml.replacePlaceholders(replacements)

        val valuesLines = values.mapIndexed { index, line ->
            """
                <tr>
                    <td style="text-align: center;">${index + 1}</td>
                    <td style="text-align: left;">${line["description"].asText()}</td>
                    <td style="text-align: right;">${formatMoney(line["unit_price"].asDouble())}</td>
                    <td style="text-align: right;">${line["quantity_executed"].asText()}</td>
                    <td style="text-align: right;">${formatMoney(line["total_price"].asDouble())}</td>
                </tr>
            """.trimIndent()
        }.joinToString("\n")

        val columnsList = columns.map { it.asText() }

        val streetColumnsHtml = columnsList.mapIndexed { index, columnName ->
            if (index == 0)
                "<th colspan=\"2\" style=\"text-align: left; font-weight: bold; min-width: 240px; max-width: 480px;\">$columnName</th>"
            else
                "<th style=\"text-align: center; font-weight: bold;width:40px;\">$columnName</th>"
        }.joinToString("")


        var dates: String? = null

        val streetLinesHtml = streets.mapIndexed { index, line ->
            val address = line[0].asText()
            val lastPower = line[1].asText()
            val items = line[2]  // ArrayNode

            val date = Utils.convertToSaoPauloLocal(Instant.parse(line[3].asText()))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val supplier = line[4].asText()

            if (index == 0) {
                dates = "Execuções realizadas de $date"
            } else if (index == streets.size() - 1) {
                dates = "$dates à $date"
            }

            val quantityCells = items.joinToString("") { "<td style=\"text-align: right;\">${it.asText()}</td>" }

            """
                <tr>
                    <td style="text-align: center;">${index + 1}</td>
                    <td style="text-align: left; min-width: 240px; max-width: 480px; word-break: break-word;">$address</td>
                    <td style="text-align: left;">$lastPower</td>
                    $quantityCells
                    <td style="text-align: right;">$date</td>
                    <td style="text-align: left;">$supplier</td>
                </tr>
            """.trimIndent()
        }.joinToString("\n")


        val streetFooterHtml = streetSums.joinToString("") {
            "<td style=\"text-align: right; font-weight: bold;\">${it.asText()}</td>"
        }

        templateHtml = templateHtml
            .replace("{{VALUE_LINES}}", valuesLines)
            .replace("{{STREET_COLUMNS}}", streetColumnsHtml)
            .replace("{{STREET_LINES}}", streetLinesHtml)
            .replace("{{STREET_FOOTER}}", streetFooterHtml)
            .replace("{{COLUMN_LENGTH}}", (columnsList.size + 1).toString())
            .replace("{{EXECUTION_DATE}}", if (dates != null && dates?.contains("null") == true) dates!! else "")

        try {
            val response = sendHtmlToPuppeteer(templateHtml)
            val responseHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_PDF
                contentDisposition = ContentDisposition.inline()
                    .filename("RELATÓRIO DE INSTALAÇÃO DE LEDS - " + contract["contract_number"].asText() + ".pdf")
                    .build()
            }

            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(response)
        } catch (e: Exception) {
            throw RuntimeException(e.message, e.cause)
        }
    }

    fun generatePhotoReport(executionId: Long): ResponseEntity<ByteArray> {
        var templateHtml = this::class.java.getResource("/templates/installation/photos.html")!!.readText()

        val data = jdbcInstallationRepository.getDataPhotoReport(executionId)
        val jsonData = data.first() // Pega o único resultado

        val company = jsonData["company"]!!
        val contract = jsonData["contract"]!!
        val streets = jsonData["streets"]!!

        val companyBucket =
            company["bucket"]?.asText() ?: throw IllegalArgumentException("Company bucket does not exist")
        val logoUri = company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo does not exist")
        val companyLogoUrl = minioService.getPresignedObjectUrl(companyBucket, logoUri)

        val streetLinesHtml = streets.joinToString("\n") { line ->
            val photoUrl = minioService.getPresignedObjectUrl(companyBucket, line["execution_photo_uri"].asText())
            """
                <div style="
                      page-break-inside: avoid;
                      margin: 20px 0;
                      border-top: 2px solid #054686;
                      border-bottom: 2px solid #054686;
                      font-family: Arial, Helvetica, sans-serif;
                    ">

                    <!-- Endereço -->
                    <p style="
                    margin: 0;
                    padding: 8px 12px;
                    text-align: center;
                    font-weight: bold;
                    font-size: 12px;
                    color: #054686;
                    border-bottom: 1px solid #054686;
                  ">
                        ${line["address"].asText()}
                    </p>

                    <!-- Coordenadas -->
                    ${
                line["latitude"]?.asText().let { latitude ->
                    """
                                <p style="
                                    margin: 0;
                                    padding: 6px 12px;
                                    text-align: center;
                                    font-size: 11px;
                                    color: #333;
                                    border-bottom: 1px solid #ccc;
                                  ">
                                    Coordenadas - Latitude: $latitude, Longitude: ${line["longitude"].asText()}
                                </p>
                            """.trimIndent()
                }
            }

                    <!-- Foto -->
                    <img
                            src="$photoUrl"
                            alt="Foto"
                            style="
                              width: 100%;
                              height: auto;
                              max-height: 85vh;
                              display: block;
                            "
                    >

                    <!-- Data -->
                    <p style="
                    margin: 0;
                    padding: 8px 12px;
                    text-align: center;
                    font-size: 11px;
                    color: #054686;
                    border-top: 1px solid #ccc;
                  ">
                        ${line["finished_at"].asText()}
                    </p>

                </div>
            """.trimIndent()
        }


        val replacements = mapOf(
            "CONTRACT_NUMBER" to contract["contract_number"].asText(),
            "COMPANY_SOCIAL_REASON" to company["social_reason"].asText(),
            "COMPANY_CNPJ" to company["company_cnpj"].asText(),
            "COMPANY_ADDRESS" to company["company_address"].asText(),
            "COMPANY_PHONE" to company["company_phone"].asText(),
            "CONTRACTOR_SOCIAL_REASON" to contract["contractor"].asText(),
            "CONTRACTOR_CNPJ" to contract["cnpj"].asText(),
            "CONTRACTOR_ADDRESS" to contract["address"].asText(),
            "CONTRACTOR_PHONE" to contract["phone"].asText(),
            "LOGO_IMAGE" to companyLogoUrl,
            "PHOTOS" to streetLinesHtml,
        )

        templateHtml = templateHtml.replacePlaceholders(replacements)


        try {
            val response = sendHtmlToPuppeteer(templateHtml, "portrait")
            val responseHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_PDF
                contentDisposition = ContentDisposition.inline()
                    .filename("RELATÓRIO FOTOGRÁFICO - CONTRATO Nº: " + contract["contract_number"].asText() + ".pdf")
                    .build()
            }

            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(response)
        } catch (e: Exception) {
            throw RuntimeException(e.message, e.cause)
        }
    }

    @Transactional
    fun cancelStep(payLoad: Map<String, Any>): ResponseEntity<Any> {
        val ids = (payLoad["currentIds"] as? List<*>)
            ?.mapNotNull { (it as? Number)?.toLong() }
            ?: emptyList()
        val type = payLoad["type"] as? String

        try {
            if (type == "DIRECT_EXECUTION") {
                namedJdbc.update(
                    """
                    delete from direct_execution_item
                    where direct_execution_id in (:ids)
                """.trimIndent(),
                    mapOf("ids" to ids)
                )

                namedJdbc.query(
                    """
                        delete from direct_execution
                        where direct_execution_id in (:ids)
                        returning reservation_management_id
                """.trimIndent(),
                    mapOf("ids" to ids)
                ) { rs, _ ->

                    val id = rs.getLong("reservation_management_id")
                    namedJdbc.update(
                        """
                        delete from reservation_management
                        where reservation_management.reservation_management_id = :id
                    """.trimIndent(),
                        mapOf("id" to id)
                    )

                }


            } else {
                throw Utils.BusinessException("Exclusão não implementada para instalações com pré-medição - Comunique ao fabricante do sistema.")
            }

        } catch (e: DataIntegrityViolationException) {
            throw Utils.BusinessException(e.message)
        }

        return ResponseEntity.noContent().build()
    }

    @Transactional
    fun archiveOrDelete(payload: Map<String, Any>): ResponseEntity<Any> {
        val directExecutionId = (payload["directExecutionId"] as Number).toLong()
        val action =
            payload["action"] as? String ?: throw Utils.BusinessException("Tente novamente - ação não recebida")

        if (action == "ARCHIVE") {
            namedJdbc.update(
                """
                    update direct_execution 
                    set direct_execution_status = 'ARCHIVED'
                    WHERE direct_execution_id = :directExecutionId
                """.trimIndent(),
                mapOf("directExecutionId" to directExecutionId)
            )
        } else {
            val uriObject: MutableSet<String> = mutableSetOf()

            namedJdbc.query(
                """
                select desi.material_stock_id, desi.contract_item_id, desi.executed_quantity, des.execution_photo_uri
                from direct_execution_street_item desi
                join direct_execution_street des on des.direct_execution_street_id = desi.direct_execution_street_id
                where des.direct_execution_id = :directExecutionId
            """.trimIndent(),
                mapOf("directExecutionId" to directExecutionId)
            ) { rs, _ ->
                val materialStockId = rs.getLong("material_stock_id")
                val contractItemId = rs.getLong("contract_item_id")
                val executedQuantity = rs.getBigDecimal("executed_quantity")
                val photoUri = rs.getString("execution_photo_uri")

                if (!photoUri.isNullOrBlank()) {
                    uriObject.add(photoUri)
                }

                namedJdbc.update(
                    """
                        update material_stock
                        set stock_quantity = stock_quantity + :quantity_executed,
                            stock_available = stock_available + :quantity_executed
                        where material_id_stock = :material_stock_id
                    """.trimIndent(),
                    mapOf(
                        "material_stock_id" to materialStockId,
                        "quantity_executed" to executedQuantity
                    )
                )

                namedJdbc.update(
                    """
                        update contract_item
                        set quantity_executed = quantity_executed - :quantity_executed
                        where contract_item_id = :contract_item_id
                    """.trimIndent(),
                    mapOf(
                        "contract_item_id" to contractItemId,
                        "quantity_executed" to executedQuantity
                    )
                )
            }

            namedJdbc.update(
                """
                    DELETE FROM direct_execution_street_item desi
                    USING direct_execution_street des
                    WHERE des.direct_execution_id = :direct_execution_id
                        AND des.direct_execution_street_id = desi.direct_execution_street_id
                """.trimIndent(),
                mapOf("direct_execution_id" to directExecutionId)
            )

            namedJdbc.update(
                """
                    delete from material_reservation
                    WHERE direct_execution_id = :direct_execution_id
                """.trimIndent(),
                mapOf("direct_execution_id" to directExecutionId)
            )

            minioService.deleteFiles("scl-construtora", uriObject)

            namedJdbc.update(
                """
                    DELETE FROM direct_execution_street
                    WHERE direct_execution_id = :direct_execution_id
                """.trimIndent(),
                mapOf("direct_execution_id" to directExecutionId)
            )

            namedJdbc.update(
                """
                    DELETE FROM direct_execution 
                    WHERE direct_execution_id = :direct_execution_id
                """.trimIndent(),
                mapOf("direct_execution_id" to directExecutionId)
            )

            namedJdbc.update(
                """
                    delete from direct_execution_executor
                    WHERE direct_execution_id = :maintenanceId
                """.trimIndent(),
                mapOf("direct_execution_id" to directExecutionId)
            )
        }

        return ResponseEntity.noContent().build()
    }

}