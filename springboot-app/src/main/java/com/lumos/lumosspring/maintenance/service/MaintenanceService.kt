package com.lumos.lumosspring.maintenance.service

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.maintenance.entities.Maintenance
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreet
import com.lumos.lumosspring.maintenance.entities.MaintenanceStreetItem
import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceStreetItemRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceStreetRepository
import com.lumos.lumosspring.minio.service.MinioService
import com.lumos.lumosspring.team.repository.TeamQueryRepository
import com.lumos.lumosspring.util.Utils
import com.lumos.lumosspring.util.Utils.getCurrentUserId
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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

    fun getGroupedMaintenances(): List<Map<String, JsonNode>> {
        return maintenanceQueryRepository.getGroupedMaintenances()
    }

    fun conventionalReport(maintenanceId: UUID, streetIds: List<UUID>): ResponseEntity<ByteArray> {
        var templateHtml = this::class.java.getResource("/templates/maintenance/conventional.html")!!.readText()

        val data = maintenanceQueryRepository.getConventionalMaintenances(maintenanceId, streetIds)
        if (data.isEmpty()) {
            throw IllegalArgumentException("Nenhum dado encontrado para os parâmetros fornecidos")
        }
        val jsonData = data.first() // Pega o único resultado

        val company = jsonData["company"]!!
        val contract = jsonData["contract"]!!
        val maintenance = jsonData["maintenance"]!!
        val streets = jsonData["streets"]!!
        val team = jsonData["team"]!!
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
            company["bucket"]?.asText() ?: throw IllegalArgumentException("Bucket ausente"),
            company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo ausente")
        )
        val dateOfVisit = Utils.convertToSaoPauloLocal(Instant.parse(maintenance["date_of_visit"].asText()))
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
            .replace("{{DATE_OF_VISIT}}", dateOfVisit.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")))
            .replace("{{ELECTRICIAN_NAME}}", "${team["electrician"]?.get("name")?.asText().orEmpty()} ${team["electrician"]?.get("last_name")?.asText().orEmpty()}")
            .replace("{{DRIVER_NAME}}", "${team["driver"]?.get("name")?.asText().orEmpty()} ${team["driver"]?.get("last_name")?.asText().orEmpty()}")

        if (maintenance.has("signature_uri") && !maintenance["signature_uri"].isNull) {
            val signatureImage = minioService.getPresignedObjectUrl(
                company["bucket"]?.asText() ?: throw IllegalArgumentException("Bucket ausente"),
                maintenance["signature_uri"]?.asText() ?: ""
            )
            val signDate = Utils.convertToSaoPauloLocal(Instant.parse(maintenance["sign_date"].asText()))

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



}