package com.lumos.lumosspring.maintenance.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.lumos.lumosspring.maintenance.dto.SendMaintenanceDTO
import com.lumos.lumosspring.maintenance.dto.MaintenanceStreetWithItems
import com.lumos.lumosspring.maintenance.model.Maintenance
import com.lumos.lumosspring.maintenance.model.MaintenanceExecutor
import com.lumos.lumosspring.maintenance.model.MaintenanceStreet
import com.lumos.lumosspring.maintenance.model.MaintenanceStreetItem
import com.lumos.lumosspring.maintenance.repository.*
import com.lumos.lumosspring.minio.service.MinioService
import com.lumos.lumosspring.util.Utils
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class MaintenanceService(
    private val maintenanceRepository: MaintenanceRepository,
    private val maintenanceStreetRepository: MaintenanceStreetRepository,
    private val maintenanceStreetItemRepository: MaintenanceStreetItemRepository,
    private val maintenanceQueryRepository: MaintenanceQueryRepository,
    private val minioService: MinioService,
    private val objectMapper: ObjectMapper,
    private val maintenanceExecutorRepository: MaintenanceExecutorRepository,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    @Transactional
    fun finishMaintenance(
        maintenance: SendMaintenanceDTO?,
        signature: MultipartFile?
    ): ResponseEntity<Any> {
        var dateOfVisit: Instant
        var signDate: Instant?

        if (maintenance == null) {
            return ResponseEntity.badRequest().body("Execution DTO está vazio.")
        }

        try {
            dateOfVisit = Instant.parse(maintenance.dateOfVisit)
            signDate = maintenance.signDate?.let { Instant.parse(it) }
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException(ex.message)
        }

        val fileUri = signature?.let {
            val folder = "photos/maintenance/${maintenance.responsible?.replace("\\s+".toRegex(), "_")}"
            minioService.uploadFile(it, Utils.getCurrentBucket(), folder, "maintenance")
        }

        val newMaintenance = Maintenance(
            maintenanceId = maintenance.maintenanceId,
            contractId = maintenance.contractId,
            pendingPoints = maintenance.pendingPoints,
            quantityPendingPoints = maintenance.quantityPendingPoints,
            dateOfVisit = dateOfVisit,
            type = maintenance.type,
            status = "FINISHED",

            signatureUri = fileUri,
            responsible = maintenance.responsible,
            signDate = signDate ?: Instant.now(),

            isNewEntry = false,
        )

        maintenanceRepository.save(newMaintenance)

        val executors = maintenance.executorsIds?.map {
            MaintenanceExecutor(
                maintenanceId = maintenance.maintenanceId,
                userId = it,
            )
        }
        if (executors != null) maintenanceExecutorRepository.saveAll(executors)

        return ResponseEntity.noContent().build()
    }

    @Transactional
    fun saveStreet(
        street: MaintenanceStreetWithItems,
    ): ResponseEntity<Any> {

        var exists = maintenanceRepository.existsById(street.street.maintenanceId)
        if (!exists) {
            val newMaintenance = Maintenance(
                maintenanceId = street.street.maintenanceId,
                contractId = null,
                pendingPoints = false,
                quantityPendingPoints = null,
                dateOfVisit = Instant.now(),
                type = "DRAFT - ${street.street.address}",
                status = "DRAFT",
            )

            maintenanceRepository.save(newMaintenance)
        }

        exists = maintenanceStreetRepository.existsById(street.street.maintenanceStreetId)
        if (exists) {
            return ResponseEntity.noContent().build()
        }

        val newStreet = MaintenanceStreet(
            maintenanceStreetId = street.street.maintenanceStreetId,
            maintenanceId = street.street.maintenanceId,
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
                maintenanceId = street.street.maintenanceId,
                maintenanceStreetId = street.street.maintenanceStreetId,
                materialStockId = it.materialStockId,
                quantityExecuted = it.quantityExecuted,
            )
        }

        maintenanceStreetItemRepository.saveAll(items)

        maintenanceQueryRepository.debitStock(street.items, street.street.maintenanceStreetId)

        return ResponseEntity.noContent().build()
    }

    fun getGroupedMaintenances(): List<Map<String, JsonNode>> {
        return maintenanceQueryRepository.getGroupedMaintenances()
    }

    fun conventionalReport(maintenanceId: UUID): ResponseEntity<ByteArray> {
        var templateHtml = this::class.java.getResource("/templates/maintenance/conventional.html")!!.readText()

        val data = maintenanceQueryRepository.getConventionalMaintenances(maintenanceId)
        if (data.isEmpty()) {
            throw IllegalArgumentException("Nenhum dado encontrado para os parâmetros fornecidos")
        }
        val jsonData = data.first() // Pega o único resultado

        val company = jsonData["company"]!!
        val contract = jsonData["contract"]!!
        val maintenance = jsonData["maintenance"]!!
        val streets = jsonData["streets"]!!
        val team = jsonData["team"]!!

        val teamArray = if (team.isArray) team as ArrayNode else objectMapper.createArrayNode()

        val teamRows = teamArray.joinToString("\n") { member ->
            val role = when (member["role"]?.asText()?.lowercase()) {
                "electrician" -> "Eletricista"
                "driver" -> "Motorista"
                "eletricista" -> "Eletricista"
                "motorista" -> "Motorista"
                else -> "Executor"
            }
            val fullName = "${member["name"]?.asText().orEmpty()} ${member["last_name"]?.asText().orEmpty()}".trim()

            """
                <tr>
                    <td>
                        <p class="label">$role:</p>
                        <p class="cell-text">$fullName</p>
                    </td>
                </tr>
            """.trimIndent()
        }

        val total_by_item = jsonData["total_by_item"]!!

        var observations = "";
        val streetsLines = streets.mapIndexed { index, line ->
            if (line.has("comment") && !line["comment"].isNull) {
                val comment = line["comment"].asText().trim().replace(Regex("\\.*$"), "")
                observations += "$comment. "
            }
            """
                <tr>
                    <td>${index + 1}</td>
                    <td>${line["address"].asText()}</td>
                    <td>${line["relay"].asText()}</td>
                    <td>${line["connection"].asText()}</td>
                    <td>${line["bulb"].asText()}</td>
                    <td>${line["sodium"].asText()}</td>
                    <td>${line["mercury"].asText()}</td>
                    <td>${line["power"].asText()}</td>
                    <td>${line["external_reactor"].asText()}</td>
                    <td>${line["internal_reactor"].asText()}</td>
                    <td>${line["relay_base"].asText()}</td>
                </tr>
            """.trimIndent()
        }.joinToString("\n")

        val companyLink = minioService.getPresignedObjectUrl(
            Utils.getCurrentBucket(),
            company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo ausente")
        )
        val dateOfVisit = Utils.convertToSaoPauloLocal(Instant.parse(maintenance["date_of_visit"].asText()))
        val hasPending = maintenance["pending_points"].asBoolean()

        val signDate =
            if (maintenance["sign_date"].asText() != "null")
                Utils.convertToSaoPauloLocal(
                    Instant.parse(maintenance["sign_date"].asText())
                )
            else
                dateOfVisit


        templateHtml = templateHtml
            .replace("{{LOGO_IMAGE}}", companyLink)
            .replace("{{CONTRACT_NUMBER}}", contract["contract_number"]?.asText() ?: "")
            .replace("{{COMPANY_SOCIAL_REASON}}", company["social_reason"]?.asText() ?: "")
            .replace("{{COMPANY_CNPJ}}", company["company_cnpj"]?.asText() ?: "")
            .replace("{{COMPANY_ADDRESS}}", company["company_address"]?.asText() ?: "")
            .replace("{{COMPANY_PHONE}}", company["company_phone"]?.asText() ?: "")
            .replace("{{CONTRACTOR_SOCIAL_REASON}}", contract["contractor"]?.asText() ?: "")
            .replace("{{CONTRACTOR_CNPJ}}", contract["cnpj"]?.asText() ?: "")
            .replace("{{CONTRACTOR_ADDRESS}}", contract["address"]?.asText() ?: "")
            .replace("{{CONTRACTOR_PHONE}}", contract["phone"]?.asText() ?: "")
            .replace("{{STREET_LINES}}", streetsLines)
            .replace("{{RELAY_TOTAL}}", total_by_item["relay"]?.asText() ?: "")
            .replace("{{CONNECTION_TOTAL}}", total_by_item["connection"]?.asText() ?: "")
            .replace("{{BULB_TOTAL}}", total_by_item["bulb"]?.asText() ?: "")
            .replace("{{SODIUM_TOTAL}}", total_by_item["sodium"]?.asText() ?: "")
            .replace("{{MERCURY_TOTAL}}", total_by_item["mercury"]?.asText() ?: "")
            .replace("{{EXTERNAL_REACTOR_TOTAL}}", total_by_item["external_reactor"]?.asText() ?: "")
            .replace("{{INTERNAL_REACTOR_TOTAL}}", total_by_item["internal_reactor"]?.asText() ?: "")
            .replace("{{BASE_TOTAL}}", total_by_item["relay_base"]?.asText() ?: "")
            .replace("{{OBSERVATIONS}}", observations)
            .replace("{{PENDING}}", if (hasPending) "checked" else "")
            .replace("{{NO_PENDING}}", if (!hasPending) "checked" else "")
            .replace("{{PENDING_QUANTITY}}", maintenance["quantity_pending_points"]?.asText() ?: "")
            .replace("{{LOCAL}}", maintenance["type"]?.asText() ?: "")
            .replace("{{DATE_OF_VISIT}}", dateOfVisit.format(DateTimeFormatter.ofPattern("dd/MM/yy 'às' HH:mm")))
            .replace(
                "{{SIGN_DATE}}",
                if (signDate != dateOfVisit) signDate.format(DateTimeFormatter.ofPattern("dd/MM/yy 'às' HH:mm")) else "Sem registro"
            )
            .replace("{{TEAM_ROWS}}", teamRows)

        if (maintenance.has("signature_uri") && !maintenance["signature_uri"].isNull) {
            val signatureImage = minioService.getPresignedObjectUrl(
                Utils.getCurrentBucket(),
                maintenance["signature_uri"]?.asText() ?: ""
            )

            val signSection =
                """
            <table >
              <thead>
                  <tr>
                      <th colspan="2" class="cell-title" style="text-align: center;">
                          ASSINATURA DO RESPONSÁVEL PELO ACOMPANHAMENTO DO SERVIÇO
                      </th>
                  </tr>
              </thead>
              <tbody>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 10px 0;">
                          <img src="$signatureImage" alt="Assinatura"
                              style="max-width: 250px; height: auto;">
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 4px;">
                          <p style="margin: 0; font-size: 10px; color: #555;">
                              Assinado digitalmente em: <strong>${signDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))}</strong>
                          </p>
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2">
                          <p class="label">Responsável:</p>
                          <p class="cell-text">${maintenance["responsible"]?.asText() ?: ""}</p>
                      </td>
                  </tr>
              </tbody>
            </table>
            """.trimIndent()
            templateHtml = templateHtml
                .replace("{{SIGN_SECTION}}", signSection)
        } else {
            templateHtml = templateHtml
                .replace("{{SIGN_SECTION}}", "")
        }

        try {
            val response = Utils.sendHtmlToPuppeteer(templateHtml)
            val responseHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_PDF
                contentDisposition = ContentDisposition.inline()
                    .filename("relatorio.pdf")
                    .build()
            }

            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(response)
        } catch (e: Exception) {
            throw RuntimeException(e.message, e.cause)
        }

    }

    fun ledReport(maintenanceId: UUID): ResponseEntity<ByteArray> {
        var templateHtml = this::class.java.getResource("/templates/maintenance/led.html")!!.readText()

        val data = maintenanceQueryRepository.getLedMaintenances(maintenanceId)
        if (data.isEmpty()) {
            throw IllegalArgumentException("Nenhum dado encontrado para os parâmetros fornecidos")
        }
        val jsonData = data.first() // Pega o único resultado

        val company = jsonData["company"]!!
        val contract = jsonData["contract"]!!
        val maintenance = jsonData["maintenance"]!!
        val streets = jsonData["streets"]!!
        val team = jsonData["team"]!!

        val teamArray = if (team.isArray) team as ArrayNode else objectMapper.createArrayNode()

        val teamRows = teamArray.joinToString("\n") { member ->
            val role = when (member["role"]?.asText()?.lowercase()) {
                "electrician" -> "Eletricista"
                "driver" -> "Motorista"
                "eletricista" -> "Eletricista"
                "motorista" -> "Motorista"
                else -> "Executor"
            }
            val fullName = "${member["name"]?.asText().orEmpty()} ${member["last_name"]?.asText().orEmpty()}".trim()

            """
                <tr>
                    <td>
                        <p class="label">$role:</p>
                        <p class="cell-text">$fullName</p>
                    </td>
                </tr>
            """.trimIndent()
        }

        val total_by_item = jsonData["total_by_item"]!!

        var observations = "";
        val streetsLines = streets.mapIndexed { index, line ->
            if (line.has("comment") && !line["comment"].isNull) {
                val comment = line["comment"].asText().trim().replace(Regex("\\.*$"), "")
                observations += "$comment. "
            }
            """
                <tr>
                    <td>${index + 1}</td>
                    <td>${line["address"].asText()}</td>
                    <td>${line["relay"].asText()}</td>
                    <td>${line["connection"].asText()}</td>
                    <td>${line["last_supply"].asText()}</td>
                    <td>${line["current_supply"].asText()}</td>
                    <td>${line["last_power"].asText()}</td>
                    <td>${line["power"].asText()}</td>
                    <td>${line["reason"].asText()}</td>
                </tr>
            """.trimIndent()
        }.joinToString("\n")

        val companyLink = minioService.getPresignedObjectUrl(
            Utils.getCurrentBucket(),
            company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo ausente")
        )
        val dateOfVisit = Utils.convertToSaoPauloLocal(Instant.parse(maintenance["date_of_visit"].asText()))
        val signDate =
            if (maintenance["sign_date"].asText() != "null")
                Utils.convertToSaoPauloLocal(
                    Instant.parse(maintenance["sign_date"].asText())
                )
            else
                dateOfVisit
        val hasPending = maintenance["pending_points"].asBoolean()

        templateHtml = templateHtml
            .replace("{{LOGO_IMAGE}}", companyLink)
            .replace("{{CONTRACT_NUMBER}}", contract["contract_number"]?.asText() ?: "")
            .replace("{{COMPANY_SOCIAL_REASON}}", company["social_reason"]?.asText() ?: "")
            .replace("{{COMPANY_CNPJ}}", company["company_cnpj"]?.asText() ?: "")
            .replace("{{COMPANY_ADDRESS}}", company["company_address"]?.asText() ?: "")
            .replace("{{COMPANY_PHONE}}", company["company_phone"]?.asText() ?: "")
            .replace("{{CONTRACTOR_SOCIAL_REASON}}", contract["contractor"]?.asText() ?: "")
            .replace("{{CONTRACTOR_CNPJ}}", contract["cnpj"]?.asText() ?: "")
            .replace("{{CONTRACTOR_ADDRESS}}", contract["address"]?.asText() ?: "")
            .replace("{{CONTRACTOR_PHONE}}", contract["phone"]?.asText() ?: "")
            .replace("{{STREET_LINES}}", streetsLines)
            .replace("{{RELAY_TOTAL}}", total_by_item["relay"]?.asText() ?: "")
            .replace("{{CONNECTION_TOTAL}}", total_by_item["connection"]?.asText() ?: "")
            .replace("{{BULB_TOTAL}}", total_by_item["bulb"]?.asText() ?: "")
            .replace("{{SODIUM_TOTAL}}", total_by_item["sodium"]?.asText() ?: "")
            .replace("{{MERCURY_TOTAL}}", total_by_item["mercury"]?.asText() ?: "")
            .replace("{{EXTERNAL_REACTOR_TOTAL}}", total_by_item["external_reactor"]?.asText() ?: "")
            .replace("{{INTERNAL_REACTOR_TOTAL}}", total_by_item["internal_reactor"]?.asText() ?: "")
            .replace("{{BASE_TOTAL}}", total_by_item["relay_base"]?.asText() ?: "")
            .replace("{{OBSERVATIONS}}", observations)
            .replace("{{PENDING}}", if (hasPending) "checked" else "")
            .replace("{{NO_PENDING}}", if (!hasPending) "checked" else "")
            .replace("{{PENDING_QUANTITY}}", maintenance["quantity_pending_points"]?.asText() ?: "")
            .replace("{{LOCAL}}", maintenance["type"]?.asText() ?: "")
            .replace("{{DATE_OF_VISIT}}", dateOfVisit.format(DateTimeFormatter.ofPattern("dd/MM/yy 'às' HH:mm")))
            .replace(
                "{{SIGN_DATE}}",
                if (signDate != dateOfVisit) signDate.format(DateTimeFormatter.ofPattern("dd/MM/yy 'às' HH:mm")) else "Sem registro"
            )
            .replace("{{TEAM_ROWS}}", teamRows)


        if (maintenance.has("signature_uri") && !maintenance["signature_uri"].isNull) {
            val signatureImage = minioService.getPresignedObjectUrl(
                Utils.getCurrentBucket(),
                maintenance["signature_uri"]?.asText() ?: ""
            )

            val signSection =
                """
            <table >
              <thead>
                  <tr>
                      <th colspan="2" class="cell-title" style="text-align: center;">
                          ASSINATURA DO RESPONSÁVEL PELO ACOMPANHAMENTO DO SERVIÇO
                      </th>
                  </tr>
              </thead>
              <tbody>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 10px 0;">
                          <img src="$signatureImage" alt="Assinatura"
                              style="max-width: 250px; height: auto;">
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 4px;">
                          <p style="margin: 0; font-size: 10px; color: #555;">
                              Assinado digitalmente em: <strong>${signDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))}</strong>
                          </p>
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2">
                          <p class="label">Responsável:</p>
                          <p class="cell-text">${maintenance["responsible"]?.asText() ?: ""}</p>
                      </td>
                  </tr>
              </tbody>
            </table>
            """.trimIndent()
            templateHtml = templateHtml
                .replace("{{SIGN_SECTION}}", signSection)
        } else {
            templateHtml = templateHtml
                .replace("{{SIGN_SECTION}}", "")
        }

        try {
            val response = Utils.sendHtmlToPuppeteer(templateHtml)
            val responseHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_PDF
                contentDisposition = ContentDisposition.inline()
                    .filename("relatorio.pdf")
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
    fun archiveOrDelete(payload: Map<String, Any>): ResponseEntity<Any> {
        val maintenanceId = UUID.fromString(payload["maintenanceId"].toString())
        val action =
            payload["action"] as? String ?: throw Utils.BusinessException("Tente novamente - ação não recebida")

        if (action == "ARCHIVE") {
            namedParameterJdbcTemplate.update(
                """
                    update maintenance 
                    set status = 'ARCHIVED'
                    WHERE maintenance_id = :maintenanceId
                """.trimIndent(),
                mapOf("maintenanceId" to maintenanceId)
            )
        } else {
            namedParameterJdbcTemplate.query(
                """
                select material_stock_id, quantity_executed, maintenance_street_id
                from maintenance_street_item
                where maintenance_id = :maintenanceId
            """.trimIndent(),
                mapOf("maintenanceId" to maintenanceId)
            ) { rs, _ ->
                val materialStockId = rs.getLong("material_stock_id")
                val maintenanceStreetId = UUID.fromString(rs.getString("maintenance_street_id"))
                val quantityExecuted = rs.getBigDecimal("quantity_executed")

                namedParameterJdbcTemplate.update(
                    """
                    delete from material_history
                    WHERE maintenance_street_id = :maintenance_street_id
                """.trimIndent(),
                    mapOf("maintenance_street_id" to maintenanceStreetId)
                )

                namedParameterJdbcTemplate.update(
                    """
                        update material_stock
                        set stock_quantity = stock_quantity + :quantity_executed,
                            stock_available = stock_available + :quantity_executed
                        where material_id_stock = :material_stock_id
                    """.trimIndent(),
                    mapOf(
                        "material_stock_id" to materialStockId,
                        "quantity_executed" to quantityExecuted
                    )
                )
            }

            namedParameterJdbcTemplate.update(
                """
                    delete from maintenance_street_item 
                    WHERE maintenance_id = :maintenanceId
                """.trimIndent(),
                mapOf("maintenanceId" to maintenanceId)
            )

            namedParameterJdbcTemplate.update(
                """
                    delete from maintenance_street
                    WHERE maintenance_id = :maintenanceId
                """.trimIndent(),
                mapOf("maintenanceId" to maintenanceId)
            )

            namedParameterJdbcTemplate.update(
                """
                    delete from maintenance 
                    WHERE maintenance_id = :maintenanceId
                """.trimIndent(),
                mapOf("maintenanceId" to maintenanceId)
            )

            namedParameterJdbcTemplate.update(
                """
                    delete from maintenance_executor 
                    WHERE maintenance_id = :maintenanceId
                """.trimIndent(),
                mapOf("maintenanceId" to maintenanceId)
            )
        }

        return ResponseEntity.noContent().build()
    }


}