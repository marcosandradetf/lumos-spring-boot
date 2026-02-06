package com.lumos.lumosspring.executionreport.installation

import com.fasterxml.jackson.databind.JsonNode
import com.lumos.lumosspring.directexecution.repository.DirectExecutionRepository
import com.lumos.lumosspring.premeasurement.repository.installation.PreMeasurementInstallationRepository
import com.lumos.lumosspring.report.controller.ReportController
import com.lumos.lumosspring.s3.service.S3Service
import com.lumos.lumosspring.util.Utils
import com.lumos.lumosspring.util.Utils.formatMoney
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class InstallationReportService(
    private val s3Service: S3Service,
    private val installationReportRepository: InstallationReportRepository,
    private val directExecutionRepository: DirectExecutionRepository,
    private val preMeasurementInstallationRepository: PreMeasurementInstallationRepository,
)  {

    fun generateDataReport(
        filtersRequest: ReportController.FiltersRequest
    ): ResponseEntity<Any> {
        var html = this::class.java.getResource("/templates/maintenance/grouped.html")!!.readText()

        val start = filtersRequest.startDate.atOffset(ZoneOffset.UTC)
        val end = filtersRequest.endDate.atOffset(ZoneOffset.UTC)
        val executionId = filtersRequest.executionId?.toLong()
        val contractId = filtersRequest.contractId

        val data = installationReportRepository.getDataForReport(
            startDate = start,
            endDate = end,
            contractId = contractId,
            installationId = executionId,
            installationType = filtersRequest.executionType
        )

        if (data.isEmpty()) {
            throw IllegalArgumentException("Nenhum dado encontrado para os parâmetros fornecidos")
        }

        val root = data.first() // Pega o único resultado

        val company = root["company"]!!
        val contract = root["contract"]!!
        val executions = root["executions"]!!

        val logoUri = company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo does not exist")
        val logoUrl = s3Service.getPresignedObjectUrl(Utils.getCurrentBucket(), logoUri)

        val titleDoc = if (filtersRequest.type == "data") "Relatório de Instalações de LEDs"
        else "Relatório Fotográfico"
        val titlePdf = if (filtersRequest.type == "led") "RELATÓRIO DE INSTALAÇÕES DE LEDS"
        else "RELATÓRIO FOTOGRÁFICO"

        html = html.replace("{{TITLE_DOC}}", titleDoc).replace("{{TITLE_PDF}}", titlePdf)
            .replace("{{LOGO_IMAGE}}", logoUrl).replace("{{CONTRACT_NUMBER}}", contract["contract_number"].asText())
            .replace("{{COMPANY_SOCIAL_REASON}}", company["social_reason"].asText())
            .replace("{{COMPANY_CNPJ}}", company["company_cnpj"].asText())
            .replace("{{COMPANY_ADDRESS}}", company["company_address"].asText())
            .replace("{{COMPANY_PHONE}}", company["company_phone"].asText())
            .replace("{{CONTRACTOR_SOCIAL_REASON}}", contract["contractor"].asText())
            .replace("{{CONTRACTOR_CNPJ}}", contract["cnpj"].asText())
            .replace("{{CONTRACTOR_ADDRESS}}", contract["address"].asText())
            .replace("{{CONTRACTOR_PHONE}}", contract["phone"].asText())

        var startDate: String? = null
        var endDate: String? = null
        val updateReportView: MutableSet<Pair<String, Long>> = mutableSetOf()
        val executionsBlock = executions.joinToString("\n") { e ->
            updateReportView.add(Pair(e["installation_type"].asText(), e["installation_id"].asLong()))

            // Início da execução
            val start = Utils.convertToSaoPauloLocal(
                Instant.parse(e.path("started_at").asText())
            )

            // Fim da execução (fallback se não houver assinatura)
            val end = if (e.hasNonNull("finished_at")) Utils.convertToSaoPauloLocal(
                Instant.parse(e.path("finished_at").asText())
            )
            else start

            // Datas formatadas (para exibição)
            val dateOfVisit = start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            if (startDate == null) {
                startDate = start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            }
            val signDate = end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            endDate = end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

            // Cálculo da duração
            val duration = Duration.between(start, end)


            // Evita duração negativa (defensivo)
            val totalMinutes = maxOf(duration.toMinutes(), 0)

            // Converte para horas + minutos
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60

            // Texto final para relatório
            val durationFormatted = "${hours}h ${minutes}min"

            val values = e["values"]
            val valuesLines = values.mapIndexed { index, it ->
                """
                    <tr>
                        <td>${index + 1}</td>
                        <td style="text-align:left">${it["description"].asText()}</td>
                        <td>${formatMoney(it["unit_price"].asDouble())}</td>
                        <td>${it["quantity_executed"].asText()}</td>
                        <td>${formatMoney(it["total_price"].asDouble())}</td>
                    </tr>
                """.trimIndent()
            }.joinToString("\n")

            val valuesTable = """
                <table class="data-table">
                    <thead>
                        <th colspan="2">ITEM CONTRATUAL</th>
                        <th>PREÇO UNITÁRIO</th>
                        <th>QUANTIDADE EXECUTADA</th>
                        <th>VALOR TOTAL</th>
                    </thead>
                    <tbody>
                        $valuesLines
                    </tbody>
                </table>
            """.trimIndent()

            val columns = e["columns"]
            val columnsList = columns.map { it.asText() }
            val streetColumns = columnsList.mapIndexed { index, columnName ->
                if (index == 0)
                    "<th colspan=\"2\">$columnName</th>"
                else
                    "<th>$columnName</th>"
            }.joinToString("")

            val streets = e["streets"]
            val streetLines = streets.mapIndexed { index, line ->
                val address = line["address"].asText()
                val lastPower = line["last_power"].asText()
                val items = line["items"]  // ArrayNode

                val date = Utils.convertToSaoPauloLocal(Instant.parse(line["finished_at"].asText()))
                    .format(DateTimeFormatter.ofPattern("dd/MM/yy"))

                val supplier = line["current_supply"].asText()

                val quantityCells = items.joinToString("") { "<td>${it.asText()}</td>" }

                """
                    <tr>
                        <td>${index + 1}</td>
                        <td style="text-align:left">$address</td>
                        <td>$lastPower</td>
                        $quantityCells
                        <td>$date</td>
                        <td style="text-align:left">${supplier.uppercase()}</td>
                    </tr>
                """.trimIndent()
            }.joinToString("\n")

            val installationsTable = """
                <table class="data-table">
                    <thead>
                        <tr>$streetColumns</tr>
                    </thead>
                    <tbody>
                        $streetLines
                    </tbody>
                </table>
            """.trimIndent()

            // team section
            val team = e["team"]
            val teamRows = team.joinToString("\n") {
                """
                    <tr>
                        <td>${it["role"].asText()}</td>
                        <td>${it["name"].asText()} ${it["last_name"].asText()}</td>
                    </tr>
                """.trimIndent()
            }
            val teamTable =
                """
                    <table class="data-table" style="margin-top:10px">
                        <thead>
                            <tr><th>Função</th><th>Nome</th></tr>
                        </thead>
                        <tbody>
                            $teamRows
                        </tbody>
                    </table>
                """.trimIndent()

            // signSection
            val signSection = if (!e["signature_uri"].isNull) {
                val signUrl = s3Service.getPresignedObjectUrl(
                    Utils.getCurrentBucket(), e["signature_uri"].asText()
                )
                """
                    <div class="signature">
                        <img src="$signUrl">
                        <div>Assinado em $signDate</div>
                    </div>
                """.trimIndent()
            } else ""

            """
                <div class="pdf-page">
                    <div class="page-content">
                        <div class="maintenance-header">
                            <div>Período: De $dateOfVisit às $signDate (Produtividade: $durationFormatted)</div>
                            ${if(!e["responsible"].isNull) "<div>Responsável pelo acompanhamento: ${e["responsible"].asText()}</div>" else ""}
                        </div>
                        
                        <div class="maintenance-body">
                            <div class="signature">
                                <h2>Valores para emissão de nota fiscal</h2>
                            </div>
                            $valuesTable
                            
                            <div class="signature">
                                <h2>Instalações realizadas</h2>
                            </div>
                            $installationsTable
                            
                            <div class="signature">
                                <h2>Equipe Executante</h2>
                            </div>
                            $teamTable
                            
                            $signSection
                        </div>
                    </div>
                </div>
            """.trimIndent()

        }

        html = html.replace("{{EXECUTIONS_BLOCK}}", executionsBlock)
        println(html)
        val pdf = Utils.sendHtmlToPuppeteer(html)

        updateReportView
            .filter { it.first == "DIRECT_EXECUTION" }
            .map { it.second }
            .takeIf { it.isNotEmpty() }
            ?.let { directExecutionRepository.registerGeneration(it) }


        updateReportView
            .filter { it.first != "DIRECT_EXECUTION" }
            .map { it.second }
            .takeIf { it.isNotEmpty() }
            ?.let { preMeasurementInstallationRepository.registerGeneration(it) }

        val responseHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            contentDisposition = ContentDisposition.attachment().filename("report.pdf").build()
        }

        return ResponseEntity.ok().headers(responseHeaders).body(pdf)
    }

    fun generatePhotoReport(executionId: Long): ResponseEntity<ByteArray> {
//        var templateHtml = this::class.java.getResource("/templates/installation/photos.html")!!.readText()
//
//        val data = directExecutionReportRepository.getDataPhotoReport(executionId)
//        val jsonData = data.first() // Pega o único resultado
//
//        val company = jsonData["company"]!!
//        val contract = jsonData["contract"]!!
//        val streets = jsonData["streets"]!!
//
//        val logoUri = company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo does not exist")
//        val companyLogoUrl = minioService.getPresignedObjectUrl(Utils.getCurrentBucket(), logoUri)
//
//        val streetLinesHtml = streets.joinToString("\n") { line ->
//            val photoUrl = minioService.getPresignedObjectUrl(Utils.getCurrentBucket(), line["execution_photo_uri"].asText())
//            """
//                <div style="
//                      page-break-inside: avoid;
//                      margin: 20px 0;
//                      border-top: 2px solid #054686;
//                      border-bottom: 2px solid #054686;
//                      font-family: Arial, Helvetica, sans-serif;
//                    ">
//
//                    <!-- Endereço -->
//                    <p style="
//                        margin: 0;
//                        padding: 8px 12px;
//                        text-align: center;
//                        font-weight: bold;
//                        font-size: 12px;
//                        color: #054686;
//                        border-bottom: 1px solid #054686;
//                      ">
//                        ${line["address"].asText()}
//                    </p>
//
//                    <!-- Coordenadas -->
//                ${
//                if (line["latitude"].asText() != "null")
//                    """
//                        <p style="
//                            margin: 0;
//                            padding: 6px 12px;
//                            text-align: center;
//                            font-size: 11px;
//                            color: #333;
//                            border-bottom: 1px solid #ccc;
//                            ">
//                            Coordenadas - Latitude: ${line["latitude"].asText()}, Longitude: ${line["longitude"].asText()}
//                        </p>
//                    """.trimIndent()
//                else ""
//            }
//
//            <!-- Foto -->
//            <img
//                    src="$photoUrl"
//                    alt="Foto"
//                    style="
//                      width: 100%;
//                      height: auto;
//                      max-height: 85vh;
//                      display: block;
//                    "
//            >
//
//            <!-- Data -->
//            <p style="
//            margin: 0;
//            padding: 8px 12px;
//            text-align: center;
//            font-size: 11px;
//            color: #054686;
//            border-top: 1px solid #ccc;
//          ">
//                ${line["finished_at"].asText()}
//            </p>
//
//        </div>
//    """.trimIndent()
//        }
//
//
//        val replacements = mapOf(
//            "CONTRACT_NUMBER" to contract["contract_number"].asText(),
//            "COMPANY_SOCIAL_REASON" to company["social_reason"].asText(),
//            "COMPANY_CNPJ" to company["company_cnpj"].asText(),
//            "COMPANY_ADDRESS" to company["company_address"].asText(),
//            "COMPANY_PHONE" to company["company_phone"].asText(),
//            "CONTRACTOR_SOCIAL_REASON" to contract["contractor"].asText(),
//            "CONTRACTOR_CNPJ" to contract["cnpj"].asText(),
//            "CONTRACTOR_ADDRESS" to contract["address"].asText(),
//            "CONTRACTOR_PHONE" to contract["phone"].asText(),
//            "LOGO_IMAGE" to companyLogoUrl,
//            "PHOTOS" to streetLinesHtml,
//        )
//
//        templateHtml = templateHtml.replacePlaceholders(replacements)
//
//
//        try {
//            val response = Utils.sendHtmlToPuppeteer(templateHtml, "portrait")
////            val responseHeaders = HttpHeaders().apply {
////                contentType = MediaType.APPLICATION_PDF
////                contentDisposition = ContentDisposition.inline()
////                    .filename("RELATÓRIO FOTOGRÁFICO - CONTRATO Nº: " + contract["contract_number"].asText() + ".pdf")
////                    .build()
////            }
//
//            val date = DateTimeFormatter
//                .ofPattern("ddMMyyyy")
//                .withZone(ZoneId.of("America/Sao_Paulo"))
//                .format(Instant.now())
//
//            val safeContract = Utils.sanitizeFilename(contract["contractor"]?.asText() ?: "")
//
//            val responseHeaders = HttpHeaders().apply {
//                contentType = MediaType.APPLICATION_PDF
//                contentDisposition = ContentDisposition
//                    .attachment()
//                    .filename("relatorio_fotografico_${safeContract}_$date.pdf")
//                    .build()
//            }
//
//            return ResponseEntity.ok()
//                .headers(responseHeaders)
//                .body(response)
//        } catch (e: Exception) {
//            throw RuntimeException(e.message, e.cause)
//        }


        TODO()
    }

    fun getInstallationsData(contractId: Long, startDate: OffsetDateTime, endDate: OffsetDateTime): List<Map<String, JsonNode>> {
        return installationReportRepository.getInstallationsData(
            contractId, startDate, endDate
        )
    }
}