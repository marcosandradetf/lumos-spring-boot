package com.lumos.lumosspring.execution.service

import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.execution.dto.*
import com.lumos.lumosspring.execution.entities.*
import com.lumos.lumosspring.execution.repository.*
import com.lumos.lumosspring.fileserver.service.MinioService
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.ReservationManagement
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.stock.repository.ReservationManagementRepository
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.*
import com.lumos.lumosspring.util.JdbcUtil.existsRaw
import com.lumos.lumosspring.util.JdbcUtil.getRawData
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository,
    private val materialStockRepository: MaterialStockRepository,
    private val notificationService: NotificationService,
    private val util: Util,
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
) {

    // delegar ao estoquista a função de GERENCIAR A RESERVA DE MATERIAIS
    @Transactional
    fun delegate(delegateDTO: DelegateDTO): ResponseEntity<Any> {
        val stockist =
            userRepository.findByUserId(UUID.fromString(delegateDTO.stockistId))
                .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(DefaultResponse("Estoquista não encontrado"))

        val streets = preMeasurementStreetRepository.getAllByPreMeasurement_PreMeasurementIdAndStep(
            delegateDTO.preMeasurementId,
            delegateDTO.preMeasurementStep
        )

        if (streets.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(DefaultResponse("Nenhuma rua foi Encontrada"))

        val existingManagement = reservationManagementRepository
            .existsByStreetsPreMeasurementPreMeasurementIdAndStreetsStep(
                delegateDTO.preMeasurementId,
                delegateDTO.preMeasurementStep
            )

        if (existingManagement) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(DefaultResponse("Já existe uma gestão de reserva para esse estoquista e essas ruas."))
        }

        val management = ReservationManagement()
        management.description = delegateDTO.description
        management.stockist = stockist

        reservationManagementRepository.save(management)

        val currentUserUUID = UUID.fromString(delegateDTO.currentUserUUID)
        for (delegateStreet in delegateDTO.street) {
            val team = teamRepository.findById(delegateStreet.teamId)
                .orElse(null) ?: throw IllegalStateException()
            val assignBy = userRepository.findByUserId(currentUserUUID)
                .orElse(null) ?: throw IllegalStateException()
            val prioritized = delegateStreet.prioritized
            val comment = delegateStreet.comment

            streets.find { it.preMeasurementStreetId == delegateStreet.preMeasurementStreetId }
                ?.assignToStockistAndTeam(team, assignBy, util.dateTime, prioritized, comment, management)
                ?: throw IllegalStateException("A rua ${delegateStreet.preMeasurementStreetId} enviada não foi encontrada")

        }

        preMeasurementStreetRepository.saveAll(streets)

        return ResponseEntity.ok().build()
    }

    @Transactional
    fun delegateDirectExecution(execution: DirectExecutionDTO): ResponseEntity<Any> {
        val stockist =
            userRepository.findByUserId(UUID.fromString(execution.stockistId))
                .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(DefaultResponse("Estoquista não encontrado"))

        val contract = contractRepository.findById(execution.contractId)
            .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(DefaultResponse("Contrato não encontrado"))

        val currentUserUUID = UUID.fromString(execution.currentUserUUID)

        val exists = JdbcUtil.getSingleRow(
            namedJdbc,
            """
                select true as result from direct_execution
                where contract_id = :contractId
                and direct_execution_status <> :status
                limit 1
            """.trimIndent(),
            mapOf(
                "contractId" to execution.contractId,
                "status" to ExecutionStatus.FINISHED
            )
        )?.get("result") as? Boolean ?: false

        if (exists) {
            throw IllegalStateException(
                """
                    Existe uma etapa em andamento para este contrato que ainda não foi finalizada. 
                    Para criar uma nova etapa, finalize a etapa atual ou acompanhe o progresso no sistema.
                """.trimIndent()
            )
        }

        val step: Int = namedJdbc.queryForObject(
            """
                    select count(direct_execution_id) + 1 as step from direct_execution
                    where contract_id = :contractId
                """.trimIndent(),
            mapOf("contractId" to execution.contractId),
            Int::class.java
        )!!

        val management = ReservationManagement()
        management.description = "Etapa $step - ${contract.contractor}"
        management.stockist = stockist
        reservationManagementRepository.save(management)


        var directExecution = DirectExecution(
            description = "Etapa $step - ${contract.contractor}",
            instructions = execution.instructions,
            contractId = execution.contractId,
            teamId = execution.teamId,
            assignedBy = currentUserUUID,
            reservationManagementId = management.reservationManagementId,
            assignedAt = util.dateTime,
        )
        directExecution = directExecutionRepository.save(directExecution)

        for (item in execution.items) {
            val ciq = contract.contractItem
                .find { it.contractItemId == item.contractItemId }

            if (ciq != null) {
                if ((ciq.contractedQuantity - ciq.quantityExecuted) < item.quantity) {
                    throw IllegalStateException("Não há saldo disponível para o item ${ciq.referenceItem.nameForImport}")
                }
                val directExecutionItem = DirectExecutionItem(
                    measuredItemQuantity = item.quantity,
                    contractItemId = ciq.contractItemId,
                    directExecutionId = directExecution.directExecutionId
                        ?: throw IllegalStateException("Id da execução não encontrado")
                )
                directExecutionItemRepository.save(directExecutionItem)
            }
        }

        notificationService.sendNotificationForUserId(
            title = "Nova Ordem Disponível",
            body = "Uma nova ordem foi atribuída a você. Acesse a tela de Gerenciamento de Reservas para iniciar o processo.",
            userId = execution.stockistId,
            time = util.dateTime,
            type = NotificationType.ALERT,
        )

        return ResponseEntity.ok().build()
    }

    fun getPendingReservesForStockist(strUserUUID: String): ResponseEntity<Any> {
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val response = mutableListOf<ReserveDTOResponse>()
        val pendingManagement = getRawData(
            namedJdbc,
            "select rm.reservation_management_id, rm.description from reservation_management rm \n" +
                    "where rm.status = :status and rm.stockist_id = :stockist_id",
            mapOf("status" to ReservationStatus.PENDING, "stockist_id" to userUUID)
        )

        for (row in pendingManagement) {
            val reservationManagementId = (row["reservation_management_id"] as? Number)?.toLong() ?: 0L
            val description = row["description"] as? String ?: ""
            val directExecutionWaiting = getRawData(
                namedJdbc,
                "select de.direct_execution_id, au.name || ' ' || au.last_name as completedName ,  t.id_team, t.team_name, d.deposit_name from direct_execution de \n" +
                        "inner join team t on t.id_team = de.team_id \n" +
                        "inner join deposit d on d.id_deposit = t.deposit_id_deposit \n" +
                        "inner join app_user au on au.user_id = de.assigned_user_id \n" +
                        "where de.reservation_management_id = :reservation_management_id \n" +
                        "and ",
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
                                AND dei.item_status = :itemStatus AND cri.type NOT IN ('SERVIÇO', 'PROJETO', 'MANUTENÇÃO')
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
                        quantity = rs.getDouble("quantity"),
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
                            AND cri.type NOT IN ('SERVIÇO', 'PROJETO', 'MANUTENÇÃO')
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
                        quantity = rs.getDouble("quantity"),
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

    fun getStockMaterialForLinking(linking: String, type: String, truckDepositName: String): ResponseEntity<Any> {
        val materials: List<MaterialInStockDTO> = if (type != "NULL" && linking != "NULL") {
            materialStockRepository.findAllByLinkingAndType(
                linking.lowercase(),
                type.lowercase(),
                truckDepositName.lowercase()
            )
        } else {
            materialStockRepository.findAllByType(type.lowercase(), truckDepositName.lowercase())
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

                val reservation = if (materialReserve.centralMaterialStockId != null) {
                    val materialStock =
                        materialStockRepository.findById(materialReserve.centralMaterialStockId) // conferir
                            .orElse(null) ?: throw IllegalStateException("Material não encontrado")

                    if (materialStock.stockAvailable < materialReserve.materialQuantity)
                        throw IllegalArgumentException("O material ${materialStock.material.materialName} não possuí estoque suficiente")

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

                    val status = if (stockistMatch) ReservationStatus.APPROVED else ReservationStatus.PENDING

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

                    namedJdbc.update(
                        """
                            UPDATE material_stock set stock_available = stock_available - :materialQuantity,
                            stock_quantity = stock_quantity - :materialQuantity
                            WHERE material_id_stock = :truckMaterialStockId
                        """.trimIndent(),
                        mapOf(
                            "materialQuantity" to materialReserve.materialQuantity,
                            "truckMaterialStockId" to materialReserve.truckMaterialStockId
                        )
                    )

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

        if (
            preMeasurementStreet.reservationManagement.streets
                .none { it.streetStatus == ExecutionStatus.WAITING_STOCKIST }
        ) {
            preMeasurementStreet.reservationManagement.status = ReservationStatus.FINISHED
            namedJdbc.update(
                """
                    update pre_measurement_street_item set item_status = :status
                    where pre_measurement_street_id = :preMeasurementStreetId
                """.trimIndent(),
                mapOf(
                    "status" to ReservationStatus.FINISHED,
                    "preMeasurementStreetId" to preMeasurementStreet.preMeasurementStreetId
                )
            )
        }

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
                "Como nenhum material foi reservado em almoxarifado de terceiros. Não será necessário aprovação. " +
                        "Mas os materiais estão pendentes de coleta pela equipe."

        } else if (reservation.any { it.status == ReservationStatus.PENDING }) {
            directExecution.directExecutionStatus = ExecutionStatus.WAITING_RESERVE_CONFIRMATION
            responseMessage =
                "Como alguns itens foram reservadas em almoxarifados de terceiros. Será necessária aprovação. " +
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
            val teamCode = team?.teamCode

            bodyMessage = """
                Por favor, aceite ou negar as reservas com urgência!
                """.trimIndent()

            if (teamCode != null)
                notificationService.sendNotificationForTeam(
                    team = teamCode,
                    title = "Existem ${reserve.size} materiais pendentes de aprovação no seu almoxarifado (${deposit?.depositName ?: ""})",
                    body = bodyMessage,
                    action = "REPLY_RESERVE",
                    time = util.dateTime,
                    type = NotificationType.ALERT,
                    persistCode = streetIdOrExecutionId
                )

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

            val teamCode = team?.teamCode ?: ""
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

            notificationService.sendNotificationForTeam(
                team = teamCode,
                title = "Existem $quantity materiais pendentes de coleta no almoxarifado ${deposit?.depositName ?: ""}",
                body = bodyMessage,
                action = "",
                time = util.dateTime,
                type = NotificationType.ALERT,
                persistCode = streetIdOrExecutionId
            )

        }

    }

    fun getReservationsByStatusAndStockist(depositId: Long, status: String): ResponseEntity<Any> {
        data class ReservationDto(
            val reserveId: Long,
            val reserveQuantity: Double,
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
                        select pms.city, c.contractor, mr.material_id_reservation, mr.reserved_quantity, mr.description, 
                        ms.material_id_stock, m.material_name, m.material_power, m.material_length, 
                        t.team_name, ms.stock_quantity, mr.status
                        from material_reservation mr
                        inner join material_stock ms on ms.material_id_stock = mr.central_material_stock_id
                        inner join material m on m.id_material = ms.material_id
                        left join direct_execution de ON mr.direct_execution_id = de.direct_execution_id 
                        left join pre_measurement_street pms on pms.pre_measurement_street_id = mr.pre_measurement_street_id
                        inner join team t on t.id_team = mr.team_id
                        inner join contract c on de.contract_id = c.contract_id 
                        where ms.deposit_id = :deposit_id and mr.status = :status
                        order by mr.material_id_reservation
                    """.trimIndent(),
            mapOf("deposit_id" to depositId, "status" to status)
        )

        val reservationsGroup = rawReservations
            .groupBy {
                (it["city"] as? String) ?: (it["contractor"] as? String) ?: "Desconhecido"
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
                        reserveId = (reserve["material_id_reservation"] as Number).toLong(),
                        reserveQuantity = (reserve["reserved_quantity"] as Number).toDouble(),
                        stockQuantity = (reserve["stock_quantity"] as Number).toDouble(),
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
        if (execution == null) throw IllegalArgumentException("Street com ID ${executionDTO.streetId} não encontrada")

        if (execution["street_status"] as String != ExecutionStatus.AVAILABLE_EXECUTION) {
            return ResponseEntity.badRequest().body("Execução já enviada")
        }

        val city: String? = execution["city"] as String?
        val folder = if (city == null) "photos"
        else "photos/$city"

        val fileUri = minioService.uploadFile(photo, "scl-construtora", folder, "execution")

        val sql = """
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
                throw IllegalArgumentException("Reserva com ID ${r.reserveId} não encontrada")
            }


            val sql = if (r.materialName.contains("led", ignoreCase = true)) {
                """
                    UPDATE contract_item ci
                    SET ci.quantity_executed = ci.quantity_executed + :quantityExecuted
                    FROM pre_measurement_street_item pmsi, contract_reference_item cri
                    WHERE (cri.item_dependency = 'LED' OR ci.contract_item_id = :contractItemId)
                        AND cri.contract_reference_item_id = ci.contract_reference_item_id
                        AND pmsi.contract_item_id = ci.contract_item_id
                """.trimIndent()
            } else if (r.materialName.contains("braço", ignoreCase = true)) {
                """
                    UPDATE contract_item ci
                    SET ci.quantity_executed = ci.quantity_executed + :quantityExecuted
                    FROM pre_measurement_street_item pmsi, contract_reference_item cri
                    WHERE (cri.item_dependency = 'BRAÇO' OR ci.contract_item_id = :contractItemId)
                        AND cri.contract_reference_item_id = ci.contract_reference_item_id
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
                    RETURNING  TRUNC((reserved_quantity - quantity_completed)::numeric, 1)  AS remaining_quantity
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

        var exists = JdbcUtil.getSingleRow(
            namedJdbc,
            "SELECT true as result FROM direct_execution_street WHERE device_street_id = :deviceStreetId AND device_id = :deviceId",
            mapOf("deviceStreetId" to executionDTO.deviceStreetId, "deviceId" to executionDTO.deviceId)
        )?.get("result") as? Boolean ?: false

        if (exists) {
            return ResponseEntity.ok().build()
        }

        var executionStreet = DirectExecutionStreet(
            lastPower = executionDTO.lastPower,
            address = executionDTO.address,
            latitude = executionDTO.latitude,
            longitude = executionDTO.longitude,
            deviceStreetId = executionDTO.deviceStreetId,
            deviceId = executionDTO.deviceId,
            finishedAt = util.dateTime,
            directExecutionId = executionDTO.directExecutionId
        )

        val folder = "photos/${executionDTO.description.replace("\\s+".toRegex(), "_")}"
        val fileUri = minioService.uploadFile(photo, "scl-construtora", folder, "execution")
        executionStreet.executionPhotoUri = fileUri

        executionStreet = directExecutionRepositoryStreet.save(executionStreet)

        for (m in executionDTO.materials) {

            val item = DirectExecutionStreetItem(
                executedQuantity = m.quantityExecuted,
                materialStockId = m.truckMaterialStockId,
                contractItemId = m.contractItemId,
                directExecutionStreetId = executionStreet.directExecutionStreetId
                    ?: throw IllegalStateException("directExecutionStreetId not setted")
            )

            directExecutionRepositoryStreetItem.save(item)

            val sql = if (m.materialName.contains("led", ignoreCase = true)) {
                """
                    UPDATE contract_item ci
                    SET ci.quantity_executed = ci.quantity_executed + :quantityExecuted
                    FROM direct_execution_item di, contract_reference_item cri
                    WHERE (cri.item_dependency = 'LED' OR ci.contract_item_id = :contractItemId)
                        AND cri.contract_reference_item_id = ci.contract_reference_item_id
                        AND di.contract_item_id = ci.contract_item_id
                """.trimIndent()
            } else if (m.materialName.contains("braço", ignoreCase = true)) {
                """
                    UPDATE contract_item ci
                    SET ci.quantity_executed = ci.quantity_executed + :quantityExecuted
                    FROM direct_execution_item di, contract_reference_item cri
                    WHERE (cri.item_dependency = 'BRAÇO' OR ci.contract_item_id = :contractItemId)
                        AND cri.contract_reference_item_id = ci.contract_reference_item_id
                        AND di.contract_item_id = ci.contract_item_id
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
                    "quantityExecuted" to m.quantityExecuted,
                    "contractItemId" to m.contractItemId
                )
            )

            exists = existsRaw(
                namedJdbc,
                """
                    SELECT 1 FROM material_reservation
                    WHERE material_id_reservation = :reserveId
                """.trimIndent(),
                mapOf("reserveId" to m.reserveId)
            )

            if (!exists) {
                throw IllegalArgumentException("Reserva com ID ${m.reserveId} não encontrada")
            }

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
    fun finishDirectExecution(directExecutionId: Long): ResponseEntity<Any> {
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

        val reservations = getRawData(
            namedJdbc,
            """
                select material_id_reservation, TRUNC((reserved_quantity - quantity_completed)::numeric, 1) as quantity_to_return, truck_material_stock_id
                from material_reservation
                where direct_execution_id = :directExecutionId
            """.trimIndent(),
            mapOf("directExecutionId" to directExecutionId)
        )

        for (reserve in reservations) {
            val quantityToReturn = (reserve["quantity_to_return"] as Number).toDouble()
            val truckMaterialId = reserve["truck_material_stock_id"] as Long
            val reservationId = reserve["material_id_reservation"] as Long

            namedJdbc.update(
                """
                        UPDATE material_reservation
                        SET status = :status
                        WHERE material_id_reservation = :reservationId
                    """.trimIndent(),
                mapOf(
                    "reservationId" to reservationId,
                    "status" to ReservationStatus.FINISHED
                )
            )

            if (quantityToReturn > 0.1) {
                namedJdbc.update(
                    """
                        UPDATE material_stock
                        SET stock_available = stock_available + :quantityToReturn,
                            stock_quantity = stock_quantity + :quantityToReturn
                        WHERE material_id_stock = :truckMaterialIdStock
                    """.trimIndent(),
                    mapOf(
                        "quantityToReturn" to quantityToReturn,
                        "truckMaterialIdStock" to truckMaterialId
                    )
                )
            }

        }

        return ResponseEntity.ok().build()

    }

}
