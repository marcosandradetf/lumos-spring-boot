package com.lumos.lumosspring.execution.service

import com.google.protobuf.LazyStringArrayList.emptyList
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.execution.dto.*
import com.lumos.lumosspring.execution.entities.DirectExecution
import com.lumos.lumosspring.execution.entities.DirectExecutionItem
import com.lumos.lumosspring.execution.entities.DirectExecutionStreet
import com.lumos.lumosspring.execution.entities.MaterialReservation
import com.lumos.lumosspring.execution.repository.DirectExecutionRepository
import com.lumos.lumosspring.execution.repository.DirectExecutionRepositoryItem
import com.lumos.lumosspring.execution.repository.DirectExecutionRepositoryStreet
import com.lumos.lumosspring.execution.repository.MaterialReservationRepository
import com.lumos.lumosspring.fileserver.service.MinioService
import com.lumos.lumosspring.notifications.service.NotificationService
import com.lumos.lumosspring.pre_measurement.entities.PreMeasurementStreet
import com.lumos.lumosspring.pre_measurement.repository.PreMeasurementStreetRepository
import com.lumos.lumosspring.stock.entities.ReservationManagement
import com.lumos.lumosspring.stock.repository.DepositRepository
import com.lumos.lumosspring.stock.repository.MaterialStockRepository
import com.lumos.lumosspring.stock.repository.ReservationManagementRepository
import com.lumos.lumosspring.team.repository.StockistRepository
import com.lumos.lumosspring.team.repository.TeamRepository
import com.lumos.lumosspring.user.UserRepository
import com.lumos.lumosspring.util.*
import jakarta.transaction.Transactional
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class ExecutionService(
    private val preMeasurementStreetRepository: PreMeasurementStreetRepository,
    private val depositRepository: DepositRepository,
    private val teamRepository: TeamRepository,
    private val materialReservationRepository: MaterialReservationRepository,
    private val materialStockRepository: MaterialStockRepository,
    private val notificationService: NotificationService,
    private val util: Util,
    private val stockistRepository: StockistRepository,
    private val userRepository: UserRepository,
    private val reservationManagementRepository: ReservationManagementRepository,
    private val minioService: MinioService,
    private val jdbcTemplate: JdbcTemplate,
    private val contractRepository: ContractRepository,
    private val directExecutionRepository: DirectExecutionRepository,
    private val directExecutionItemRepository: DirectExecutionRepositoryItem,
    private val directExecutionRepositoryStreet: DirectExecutionRepositoryStreet,

    ) {
    // delegar ao estoquista a função de GERENCIAR A RESERVA DE MATERIAIS
    @Transactional
    fun delegate(delegateDTO: DelegateDTO): ResponseEntity<Any> {
        val stockist =
            userRepository.findByIdUser(UUID.fromString(delegateDTO.stockistId))
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
            val assignBy = userRepository.findByIdUser(currentUserUUID)
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
            userRepository.findByIdUser(UUID.fromString(execution.stockistId))
                .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(DefaultResponse("Estoquista não encontrado"))

        val contract = contractRepository.findById(execution.contractId)
            .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(DefaultResponse("Contrato não encontrado"))

        val team = teamRepository.findById(execution.teamId)
            .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(DefaultResponse("Equipe não encontrada"))

        val currentUserUUID = UUID.fromString(execution.currentUserUUID)

        val user = userRepository.findByIdUser(currentUserUUID)
            .orElse(null) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(DefaultResponse("Usuário atual não encontrado"))

        val management = ReservationManagement()
        management.description = contract.contractor
        management.stockist = stockist
        reservationManagementRepository.save(management)

        val directExecution = DirectExecution()
        directExecution.contract = contract
        directExecution.instructions = execution.instructions
        directExecution.team = team
        directExecution.assignedBy = user
        directExecution.reservationManagement = management
        directExecutionRepository.save(directExecution)

        for (item in execution.items) {
            val ciq = contract.contractItemsQuantitative
                .find { it.contractItemId == item.contractItemId }

            if (ciq != null) {
                if ((ciq.contractedQuantity - ciq.quantityExecuted) < item.quantity) {
                    throw IllegalStateException("Não há saldo disponível para o item ${ciq.referenceItem.nameForImport}")
                }
                val directExecutionItem = DirectExecutionItem()
                directExecutionItem.contractItem = ciq
                directExecutionItem.measuredItemQuantity = item.quantity
                directExecutionItem.directExecution = directExecution
                directExecutionItemRepository.save(directExecutionItem)
            }
        }

        return ResponseEntity.ok().build()
    }

    fun getPendingReservesForStockist(strUserUUID: String): ResponseEntity<Any> {
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val reserves =
            reservationManagementRepository.findAllByStatusAndStockistIdUser(ReservationStatus.PENDING, userUUID)
        val response = mutableListOf<ReserveDTOResponse>()

        for (reserve in reserves) {
            val description = reserve.description ?: ""
            if (reserve.directExecutions.isNotEmpty()) {
                val directExecutions = reserve.directExecutions
                    .filter { it.directExecutionStatus == ExecutionStatus.WAITING_STOCKIST }
                    .map { execution ->
                        val items = execution.directItems
                            .map { directItem ->
                                val ref = directItem.contractItem.referenceItem
                                ItemResponseDTO(
                                    itemId = directItem.directExecutionItemId,
                                    description = ref.nameForImport ?: "",
                                    quantity = directItem.measuredItemQuantity,
                                    type = ref.type,
                                    linking = ref.linking
                                )
                            }

                        ReserveStreetDTOResponse(
                            preMeasurementStreetId = 0,
                            streetName = "",
                            latitude = null,
                            longitude = null,
                            prioritized = false,
                            comment = "DIRECT_EXECUTION",
                            assignedBy = execution.assignedBy.completedName,
                            teamId = execution.team.idTeam,
                            teamName = execution.team.teamName,
                            truckDepositName = execution.team.deposit?.depositName ?: "",
                            items = items
                        )
                    }

                response.add(ReserveDTOResponse(description, directExecutions))
            } else {
                val streets = reserve.streets
                    .sortedBy { it.prioritized == false }
                    .filter { it.streetStatus == ExecutionStatus.WAITING_STOCKIST }
                    .map { street ->
                        val items = street.items
                            .filter { item ->
                                val type = item.contractItem.referenceItem.type
                                !listOf("SERVIÇO", "PROJETO").contains(type)
                            }
                            .map { item ->
                                val ref = item.contractItem.referenceItem
                                ItemResponseDTO(
                                    itemId = item.preMeasurementStreetItemId,
                                    description = ref.nameForImport ?: "",
                                    quantity = item.measuredItemQuantity,
                                    type = ref.type,
                                    linking = ref.linking
                                )
                            }

                        ReserveStreetDTOResponse(
                            preMeasurementStreetId = street.preMeasurementStreetId,
                            streetName = street.street,
                            latitude = street.latitude,
                            longitude = street.longitude,
                            prioritized = street.prioritized,
                            comment = street.comment,
                            assignedBy = street.assignedBy.completedName,
                            teamId = street.team?.idTeam ?: 0L,
                            teamName = street.team?.teamName ?: "",
                            truckDepositName = street.team?.deposit?.depositName ?: "",
                            items = items
                        )
                    }

                response.add(ReserveDTOResponse(description, streets))
            }
        }

        return ResponseEntity.ok(response)
    }

    fun getStockMaterialForLinking(linking: String, type: String, truckDepositName: String): ResponseEntity<Any> {
        var materials: List<MaterialInStockDTO>

        if (type != "NULL" && linking != "NULL") {
            materials = materialStockRepository.findAllByLinkingAndType(
                linking.lowercase(),
                type.lowercase(),
                truckDepositName.lowercase()
            )
        } else {
            materials = materialStockRepository.findAllByType(type.lowercase(), truckDepositName.lowercase())
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


        val reservation = mutableListOf<MaterialReservation>()
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            IllegalArgumentException(e.message)
        }

        for (item in executionReserve.items) {
            for (materialReserve in item.materials) {
                val materialStock = materialStockRepository.findById(materialReserve.materialId)
                    .orElse(null) ?: throw IllegalStateException("Material não encontrado")

                if (materialStock.stockAvailable < materialReserve.materialQuantity)
                    throw IllegalArgumentException("O material ${materialStock.material.materialName} não possuí estoque suficiente")

                val deposit = materialStock.deposit
                val stockistMatch = deposit.stockists.any { it.user.idUser == userUUID }
                val teamMatch = materialStock.deposit.teams.isNotEmpty()
                val contractItemId = util.getDescription(
                    field = "contract_item_id",
                    table = "tb_pre_measurements_streets_items",
                    where = "pre_measurement_street_item_id",
                    equal = item.itemId.toString(),
                    type = Long::class.java,
                )

                if (contractItemId == null) throw IllegalStateException("Contrato do item ${item.itemId} enviado não foi encontrado")

                reservation.add(
                    MaterialReservation().apply {
                        this.materialStock = materialStock
                        this.reservedQuantity = materialReserve.materialQuantity
                        if (preMeasurementStreet != null) {
                            this.description = preMeasurementStreet.street
                            this.street = preMeasurementStreet
                            this.team = preMeasurementStreet.team
                        } else if (directExecution != null) {
                            this.description = directExecution.contract.contractor
                            this.directExecution = directExecution
                            this.team = directExecution.team
                        }
                        this.contractItemId = contractItemId
                        if (teamMatch || stockistMatch) {
                            this.confirmReservation()
                            if (teamMatch) this.status = ReservationStatus.COLLECTED
                        }
                    }
                )
            }
        }

        materialReservationRepository.saveAll(reservation)

        val responseMessage =
            if (preMeasurementStreet != null) verifyStreetReservations(reservation, preMeasurementStreet)
            else if (directExecution != null) verifyDirectExecutionReservations(reservation, directExecution) else ""

        return ResponseEntity.ok().body(DefaultResponse(responseMessage))
    }

    private fun verifyStreetReservations(
        reservation: List<MaterialReservation>,
        preMeasurementStreet: PreMeasurementStreet
    ): String {
        var responseMessage = ""
        if (reservation.all { it.status == ReservationStatus.COLLECTED }) {
            preMeasurementStreet.streetStatus = ExecutionStatus.AVAILABLE_EXECUTION
            responseMessage =
                "Como todos os itens estão no caminhão, nenhuma ação adicional será necessária. A equipe pode iniciar a execução."

        } else if (!reservation.any { it.status == ReservationStatus.PENDING }) {
            preMeasurementStreet.streetStatus = ExecutionStatus.AVAILABLE_EXECUTION
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

        if (preMeasurementStreet.reservationManagement.streets
                .none { it.streetStatus == ExecutionStatus.WAITING_STOCKIST }
        ) {
            preMeasurementStreet.reservationManagement.status = ReservationStatus.FINISHED
        }

        preMeasurementStreetRepository.save(preMeasurementStreet)

        return responseMessage
    }

    private fun verifyDirectExecutionReservations(
        reservation: List<MaterialReservation>,
        directExecution: DirectExecution
    ): String {
        var responseMessage = ""
        if (reservation.all { it.status == ReservationStatus.COLLECTED }) {
            directExecution.directExecutionStatus = ExecutionStatus.AVAILABLE_EXECUTION
            responseMessage =
                "Como todos os itens estão no caminhão, nenhuma ação adicional será necessária. A equipe pode iniciar a execução."

        } else if (!reservation.any { it.status == ReservationStatus.PENDING }) {
            directExecution.directExecutionStatus = ExecutionStatus.AVAILABLE_EXECUTION
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

        directExecution.reservationManagement.status = ReservationStatus.FINISHED

        directExecutionRepository.save(directExecution)

        return responseMessage
    }


    private fun notify(reservation: List<MaterialReservation>, streetIdOrExecutionId: String) {
        var bodyMessage: String
        val companyPhone = util.getDescription(
            field = "company_phone",
            table = "tb_companies",
            type = String::class.java,
        )

        val groupedByDepositPending = reservation
            .filter { it.status == ReservationStatus.PENDING }
            .groupBy { it.materialStock?.deposit }

        val groupedByDepositApproved = reservation
            .filter { it.status == ReservationStatus.APPROVED }
            .groupBy { it.materialStock?.deposit }

        // Itera sobre os grupos e envia notificação para equipe por depósito
        groupedByDepositPending.forEach { (deposit, reserve) ->
            val teamCodes = deposit?.teams?.map { it.teamCode } ?: emptyList()

            bodyMessage = """
                Por favor, aceite ou negar as reservas com urgência!
                """.trimIndent()

            teamCodes.forEach { it ->
                notificationService.sendNotificationForTeam(
                    team = it,
                    title = "Existem ${reserve.size} materiais pendentes de aprovação no seu almoxarifado (${deposit?.depositName ?: ""})",
                    body = bodyMessage,
                    action = "REPLY_RESERVE",
                    time = util.dateTime,
                    type = NotificationType.ALERT,
                    persistCode = streetIdOrExecutionId
                )
            }

        }

        // Itera sobre os grupos e envia notificação para equipe por depósito
        groupedByDepositApproved.forEach { (deposit, reserve) ->
            val teamCode = reserve.first().team?.teamCode ?: ""
            val quantity = reserve.size

            val depositName = deposit?.depositName ?: "Desconhecido"
            val address = deposit?.depositAddress ?: "Endereço não informado"
            val phone = deposit?.depositPhone ?: companyPhone ?: "Telefone não Informado"
            val responsible = deposit?.stockists
                ?.firstOrNull()
                ?.user
                ?.completedName
                ?: "Responsável não informado"

            val bodyMessage = """
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

    fun getReservationsByStatusAndStockist(strUserUUID: String, status: String): ResponseEntity<Any> {
        val userUUID = try {
            UUID.fromString(strUserUUID)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val stockists = stockistRepository.findByUserUUID(userUUID).distinctBy { it.user.idUser }

        data class ReservationDto(
            val reserveId: Long,
            val reserveQuantity: Double,
            val materialName: String,
            val description: String?,
            val teamId: Long?,
            val teamName: String?
        )

        data class ReservationsByPreMeasurementDto(
            val preMeasurementName: String,
            val reservations: List<ReservationDto>
        )

        val response: MutableList<ReservationsByPreMeasurementDto> = mutableListOf()

        for (stockist in stockists) {
            val materials = stockist.deposit.materialStocks
            for (material in materials) {
                var materialName = material.material.materialName
                val power: String? = material.material.materialPower
                val length: String? = material.material.materialLength
                materialName += if (power != null) " $power" else " $length"

                val reservationsGroup = material.reservations
                    .filter { it.status == status }
                    .groupBy { it.street?.preMeasurement?.city ?: it.directExecution?.contract?.contractor ?: "Desconhecido" }

                for ((preMeasurementName, reservations) in reservationsGroup) {
                    val list = mutableListOf<ReservationDto>()
                    for (reserve in reservations) {
                        list.add(
                            ReservationDto(
                                reserveId = reserve.materialIdReservation,
                                reserveQuantity = reserve.reservedQuantity,
                                materialName = materialName,
                                description = reserve.description,
                                teamId = reserve.team.idTeam,
                                teamName = reserve.team.teamName,
                            )
                        )
                    }
                    response.add(
                        ReservationsByPreMeasurementDto(
                            preMeasurementName = preMeasurementName,
                            reservations = list
                        )
                    )
                }


            }
        }


        return ResponseEntity.ok().body(response)
    }

    @Transactional
    fun uploadIndirectExecution(photo: MultipartFile, executionDTO: SendExecutionDto?): ResponseEntity<Any> {
        if (executionDTO == null) {
            return ResponseEntity.badRequest().body("Execution DTO está vazio.")
        }

        val execution = preMeasurementStreetRepository.findById(executionDTO.streetId)
            .orElseThrow { IllegalArgumentException("Street com ID ${executionDTO.streetId} não encontrada") }

        if (execution.streetStatus != ExecutionStatus.AVAILABLE_EXECUTION) {
            return ResponseEntity.badRequest().body("Execução já enviada")
        }

        val city: String? = execution.city
        val folder = if (city == null) "photos"
        else "photos/$city"

        val fileUri = minioService.uploadFile(photo, "scl-construtora", folder, "execution")

        execution.executionPhotoUri = fileUri
        execution.streetStatus = ExecutionStatus.FINISHED
        preMeasurementStreetRepository.save(execution)

        for (r in executionDTO.reserves) {
            val reserve = materialReservationRepository.findById(r.reserveId)
                .orElseThrow { IllegalArgumentException("Reserva com ID ${r.reserveId} não encontrada") }

            reserve.setQuantityCompleted(r.quantityExecuted)
            materialReservationRepository.save(reserve)

            val contractItemId = reserve.contractItemId
            val sql = "UPDATE tb_contracts_items set quantity_executed = ? where contract_item_id = ?"
            jdbcTemplate.update(sql, r.quantityExecuted, contractItemId)
        }


        return ResponseEntity.ok().build()
    }

    @Transactional
    fun uploadDirectExecution(photo: MultipartFile, executionDTO: SendDirectExecutionDto?): ResponseEntity<Any> {

        if (executionDTO == null) {
            return ResponseEntity.badRequest().body("Execution DTO está vazio.")
        }

        val sql = "SELECT 1 FROM tb_direct_executions_streets WHERE deviceStreetId = ? AND deviceId = ?"
        val exists = try {
            jdbcTemplate.queryForObject(sql, Int::class.java, executionDTO.deviceStreetId, executionDTO.deviceId)
            true
        } catch (ex: EmptyResultDataAccessException) {
            false
        }

        if (exists) {
            return ResponseEntity.ok().build()
        }

        val executionStreet = DirectExecutionStreet()
        executionStreet.lastPower = executionDTO.lastPower
        executionStreet.streetName = executionDTO.streetName
        executionStreet.number = executionDTO.number
        executionStreet.neighborhood = executionDTO.hood
        executionStreet.city = executionDTO.city
        executionStreet.latitude = executionDTO.latitude
        executionStreet.longitude = executionDTO.longitude
        executionStreet.deviceStreetId = executionDTO.deviceStreetId
        executionStreet.deviceId = executionDTO.deviceId
        executionStreet.finishedAt = util.dateTime
        val folder = "photos/${executionDTO.city}"
        val fileUri = minioService.uploadFile(photo, "scl-construtora", folder, "execution")
        executionStreet.executionPhotoUri = fileUri

        directExecutionRepositoryStreet.save(executionStreet)

        for (r in executionDTO.reserves) {
            val reserve = materialReservationRepository.findById(r.reserveId)
                .orElseThrow { IllegalArgumentException("Reserva com ID ${r.reserveId} não encontrada") }

            reserve.setQuantityCompleted(r.quantityExecuted)
            materialReservationRepository.save(reserve)

            val contractItemId = reserve.contractItemId
            val sql = "UPDATE tb_contracts_items set quantity_executed = ? where contract_item_id = ?"
            jdbcTemplate.update(sql, r.quantityExecuted, contractItemId)
        }

        return ResponseEntity.ok().build()
    }

    fun getIndirectExecutions(strUUID: String?): ResponseEntity<MutableList<IndirectExecutionDTO>> {

        val uuid = strUUID ?: return ResponseEntity.badRequest().body(arrayListOf())

        val user = userRepository.findById(UUID.fromString(uuid))
            .orElseThrow { IllegalArgumentException("Equipe não encontrada") }

        val teamsId = when {
            user.electricians.isNotEmpty() -> user.electricians.map { it.idTeam }
            user.drivers.isNotEmpty() -> user.drivers.map { it.idTeam }
            else -> return ResponseEntity.badRequest().body(arrayListOf())
        }

        val streets = preMeasurementStreetRepository.findByTeam_IdTeam(teamsId)

        val reservationsByStreet = materialReservationRepository
            .findAllToIndirectExecution(streets.map { it.streetId })
            .filter { it.street != null }
            .groupBy { it.street!!.preMeasurementStreetId }

        val reservesByStreet = reservationsByStreet.mapValues { (_, reservations) ->
            reservations
                .filter { it.street != null }
                .map { r ->
                    val materialStock = r.materialStock
                    val deposit = materialStock?.deposit
                    val stockist = deposit?.stockists?.firstOrNull()?.user

                    var name = materialStock?.material?.materialName
                    val length = materialStock?.material?.materialLength
                    val power = materialStock?.material?.materialPower
                    if (power != null) {
                        name += " $power"
                    } else if (length != null) {
                        name += " $length"
                    }

                    Reserve(
                        reserveId = r.materialIdReservation,
                        materialName = name ?: "",
                        materialQuantity = r.reservedQuantity,
                        reserveStatus = r.status,
                        streetId = r.street!!.preMeasurementStreetId,
                        depositId = deposit?.idDeposit ?: 0L,
                        depositName = deposit?.depositName ?: "Desconhecido",
                        depositAddress = deposit?.depositAddress ?: "Desconhecido",
                        stockistName = stockist?.completedName ?: "Desconhecido",
                        phoneNumber = stockist?.phoneNumber ?: "Desconhecido",
                        requestUnit = materialStock?.requestUnit ?: "UN"
                    )

                }
        }

        val executions = streets.mapNotNull { street ->
            val reserves = reservesByStreet[street.streetId] ?: listOf()

            // Verifica se existe algum item PENDENTE
            val hasPending = reserves.any { it.reserveStatus == ReservationStatus.PENDING }

            if (hasPending) {
                // Ignora essa rua
                null
            } else {
                IndirectExecutionDTO(
                    streetId = street.streetId,
                    streetName = street.streetName,
                    streetNumber = street.streetNumber,
                    streetHood = street.streetHood,
                    city = street.city,
                    state = street.state,
                    teamName = street.teamName,
                    priority = street.priority,
                    type = street.type,
                    itemsQuantity = street.itemsQuantity,
                    creationDate = street.creationDate.toString(),
                    latitude = street.latitude,
                    longitude = street.longitude,
                    contractId = street.contractId,
                    contractor = street.contractor,
                    reserves = reserves
                )
            }
        }.toMutableList()


        return ResponseEntity.ok().body(executions)

    }

    fun getDirectExecutions(strUUID: String?): ResponseEntity<MutableList<DirectExecutionDTOResponse>> {
        val uuid = strUUID ?: return ResponseEntity.badRequest().body(arrayListOf())

        val user = userRepository.findById(UUID.fromString(uuid))
            .orElseThrow { IllegalArgumentException("Equipe não encontrada") }

        val teamsId = when {
            user.electricians.isNotEmpty() -> user.electricians.map { it.idTeam }
            user.drivers.isNotEmpty() -> user.drivers.map { it.idTeam }
            else -> return ResponseEntity.badRequest().body(arrayListOf())
        }

        val reservations = materialReservationRepository.findAllToDirectExecution(teamsId)
        val contracts = reservations.map { it.directExecution!!.contract }
        val reservationsGroupByContract = reservations
            .groupBy { it.directExecution!!.contract.contractId }

        val reservationsByContract = reservationsGroupByContract.mapValues { (_, reservations) ->
            reservations
                .map { r ->
                    val materialStock = r.materialStock
                    val deposit = materialStock?.deposit
                    val stockist = deposit?.stockists?.firstOrNull()?.user

                    var name = materialStock?.material?.materialName
                    val length = materialStock?.material?.materialLength
                    val power = materialStock?.material?.materialPower
                    if (power != null) {
                        name += " $power"
                    } else if (length != null) {
                        name += " $length"
                    }

                    Reserve(
                        reserveId = r.materialIdReservation,
                        materialName = name ?: "",
                        materialQuantity = r.reservedQuantity,
                        reserveStatus = r.status,
                        streetId = r.street!!.preMeasurementStreetId,
                        depositId = deposit?.idDeposit ?: 0L,
                        depositName = deposit?.depositName ?: "Desconhecido",
                        depositAddress = deposit?.depositAddress ?: "Desconhecido",
                        stockistName = stockist?.completedName ?: "Desconhecido",
                        phoneNumber = stockist?.phoneNumber ?: "Desconhecido",
                        requestUnit = materialStock?.requestUnit ?: "UN"
                    )

                }
        }

        val executions = contracts.mapNotNull { contract ->
            val reserves = reservationsByContract[contract.contractId] ?: listOf()

            // Verifica se existe algum item PENDENTE
            val hasPending = reserves.any { it.reserveStatus == ReservationStatus.PENDING }

            if (hasPending) {
                // Ignora esse contrato
                null
            } else {
                DirectExecutionDTOResponse(
                    contractId = contract.contractId,
                    contractor = contract.contractor ?: "",
                    instructions = contract.directExecutions.first().instructions ?: "",
                    reserves = reserves
                )
            }
        }.toMutableList()

        return ResponseEntity.ok().body(executions)
    }


//
//    fun reservationsReply(
//        approvals: List<ReplyReserveDTO>,
//        declinations: List<ReplyReserveDTO>,
//        streetId: Long,
//        userUUID: UUID
//    ): ResponseEntity<Any> {
//
//        val reservations = materialReservationRepository
//            .findAllByStreetPreMeasurementStreetId(streetId)
//            .orElse(emptyList())
//
//        val approvedIds = approvals.map { it.reserveId }.toSet()
//        val declinedIds = declinations.map { it.reserveId }.toSet()
//
//        val team = teamRepository.findByUserUUID(userUUID).orElse(null)
//        val stockist = stockistRepository.findByUserUUID(userUUID)
//        val firstDeposit = reservations.firstOrNull()?.firstDepositCity
//
//        val isTeam = team != null && reservations.any { it.team == team }
//
//        for (reserve in reservations) {
//            when {
//                stockist.isPresent -> {
//                    val isFirst = stockist.get().deposit == firstDeposit
//                    val location =
//                        if (isFirst) MaterialReservation.Location.FIRST else MaterialReservation.Location.SECOND
//
//                    when (reserve.idMaterialReservation) {
//                        in approvedIds -> reserve.confirmReservation(location)
//                        in declinedIds -> reserve.rejectReservation(location)
//                    }
//                }
//
//                isTeam -> {
//                    val location = MaterialReservation.Location.TRUCK
//                    when (reserve.idMaterialReservation) {
//                        in approvedIds -> reserve.confirmReservation(location)
//                        in declinedIds -> reserve.rejectReservation(location)
//                    }
//                }
//
//                else -> {
//                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(ErrorResponse("Usuário não tem permissão para responder a essa reserva"))
//                }
//            }
//        }
//
//        materialReservationRepository.saveAll(reservations)
//
//        return ResponseEntity.ok().build()
//    }

}
