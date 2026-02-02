package com.lumos.lumosspring.report.service

import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceQueryRepository
import com.lumos.lumosspring.minio.service.MinioService
import com.lumos.lumosspring.report.controller.ReportController
import com.lumos.lumosspring.util.Utils
import org.springframework.http.*
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class ReportService(
    private val maintenanceRepository: MaintenanceQueryRepository,
    private val contractRepository: ContractRepository,
    private val minioService: MinioService,
) {
    fun generatePdf(htmlRequest: String, title: String): ResponseEntity<ByteArray?> {
        return try {
            val html = """
            <!DOCTYPE html>
            <html lang="pt">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>$title</title>
            
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        font-size: 12px;
                        background-color: #ffffff;
                    }
                
                    h1, h2 {
                        color: #333;
                    }
                
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 0.9em;
                    }
                
                    th, td {
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: left;
                    }
                
                    th {
                        background-color: #044686;
                        color: white;
                    }
                
                    .report-total-sum, .report-base-total {
                        font-weight: bold;
                        background-color: #eee;
                    }
                
                    .footer {
                        margin-top: 20px;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
            $htmlRequest
            </body></html>
        """.trimIndent()

            // Criar processo para executar wkhtmltopdf
            val processBuilder = ProcessBuilder(
                "wkhtmltopdf",
                "--quiet",
                "--enable-smart-shrinking",
                "--dpi", "300",
                "--page-width", "252mm",  // 20% maior que A4
                "--page-height", "297mm", // Altura padrão do A4
                "--print-media-type",     // Garante que o background apareça
                "--margin-top", "10mm",
                "--margin-right", "5mm",
                "--margin-bottom", "10mm",
                "--margin-left", "5mm",
                "-", "-"
            )

            val process = processBuilder.start()

            // Enviar HTML via entrada padrão (stdin)
            BufferedWriter(OutputStreamWriter(process.outputStream)).use { writer ->
                writer.write(html)
                writer.flush()
            }

            // Capturar saída do wkhtmltopdf (PDF gerado)
            val pdfBytes = process.inputStream.readBytes()

            // Esperar o processo terminar e verificar sucesso
            if (process.waitFor() != 0) {
                throw RuntimeException("Erro ao gerar PDF com wkhtmltopdf")
            }

            // Configurar cabeçalhos da resposta
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_PDF
            headers.setContentDisposition(ContentDisposition.attachment().filename("relatorio-$title.pdf").build())

            ResponseEntity(pdfBytes, headers, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }

    fun generateExecutionReport(filtersRequest: ReportController.FiltersRequest): ResponseEntity<Any> {
        return if (filtersRequest.scope == "MAINTENANCE") {
            generateMaintenanceReport(filtersRequest)
        } else {
            generateGroupedInstallationReport(filtersRequest)
        }
    }

    private fun generateMaintenanceReport(filtersRequest: ReportController.FiltersRequest): ResponseEntity<Any> {
        return if (filtersRequest.viewMode == "LIST") {

            val start = filtersRequest.startDate.atOffset(ZoneOffset.UTC)
            val end = filtersRequest.endDate.atOffset(ZoneOffset.UTC)

            val response = maintenanceRepository.getGroupedMaintenances(
                filtersRequest.contractId,
                start,
                end,
                "%${filtersRequest.type}%"
            )

            ResponseEntity.ok().body(response)
        } else {
            generateGroupedMaintenanceReport(filtersRequest)
        }
    }

    private fun generateGroupedMaintenanceReport(filtersRequest: ReportController.FiltersRequest): ResponseEntity<Any> {
        when (filtersRequest.type) {
            "led" -> {
                var html =
                    this::class.java.getResource("/templates/maintenance/conventional_grouped.html")!!.readText()

                val start = filtersRequest.startDate.atOffset(ZoneOffset.UTC)
                val end = filtersRequest.endDate.atOffset(ZoneOffset.UTC)

                val data = maintenanceRepository
                    .getGroupedConventionalMaintenances(start, end, filtersRequest.contractId)

                if (data.isEmpty()) {
                    throw IllegalArgumentException("Nenhum dado encontrado para os parâmetros fornecidos")
                }

                val root = data.first()
                val company = root["company"]!!
                val contract = root["contract"]!!
                val maintenances = root["maintenances"]!!

                val logoUrl = minioService.getPresignedObjectUrl(
                    Utils.getCurrentBucket(),
                    company["company_logo"].asText()
                )

                html = html
                    .replace("{{LOGO_IMAGE}}", logoUrl)
                    .replace("{{CONTRACT_NUMBER}}", contract["contract_number"].asText())
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
                val maintenanceBlocks = maintenances.joinToString("\n") { m ->

                    val dateOfVisit = Utils.convertToSaoPauloLocal(
                        Instant.parse(m["date_of_visit"].asText())
                    ).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

                    if (startDate == null) {
                        startDate = dateOfVisit
                    }

                    val signDate = if (!m["sign_date"].isNull)
                        Utils.convertToSaoPauloLocal(
                            Instant.parse(m["sign_date"].asText())
                        ).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    else "—"
                    endDate = signDate

                    val streets = m["streets"]
                    val team = m["team"]

                    val streetRows = streets.mapIndexed { i, s ->
                        """
                            <tr>
                                <td>${i + 1}</td>
                                <td class="left">${s["address"].asText()}</td>
                                <td>${s["relay"].asText()}</td>
                                <td>${s["connection"].asText()}</td>
                                <td>${s["bulb"].asText()}</td>
                                <td>${s["sodium"].asText()}</td>
                                <td>${s["mercury"].asText()}</td>
                                <td>${s["power"].asText()}</td>
                                <td>${s["external_reactor"].asText()}</td>
                                <td>${s["internal_reactor"].asText()}</td>
                                <td>${s["relay_base"].asText()}</td>
                            </tr>
                            """.trimIndent()
                    }.joinToString("\n")

                    val observations = streets
                        .filter { it.has("comment") && !it["comment"].isNull }
                        .joinToString(" ") { it["comment"].asText() }

                    val teamRows = team.joinToString("\n") {
                        """
                            <tr>
                                <td>${it["role"].asText()}</td>
                                <td>${it["name"].asText()} ${it["last_name"].asText()}</td>
                            </tr>
                        """.trimIndent()
                    }

                    val signSection =
                        if (!m["signature_uri"].isNull) {
                            val signUrl = minioService.getPresignedObjectUrl(
                                Utils.getCurrentBucket(),
                                m["signature_uri"].asText()
                            )
                            """
                                <div class="signature">
                                    <img src="$signUrl">
                                    <div>Assinado em $signDate</div>
                                </div>
                                """.trimIndent()
                        } else ""

                    """
                        <div class="maintenance">
                            <div class="maintenance-header">
                                <div>Data: $dateOfVisit</div>
                                <div>Tipo: ${m["type"].asText()} | Responsável: ${m["responsible"].asText()}</div>
                            </div>
                
                            <div class="maintenance-body">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Nº</th><th>Endereço</th><th>Relé</th><th>Conexão</th>
                                            <th>Lâmp.</th><th>Sódio</th><th>Merc.</th>
                                            <th>Pot.</th><th>Reator Ext.</th><th>Reator Int.</th><th>Base</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        $streetRows
                                    </tbody>
                                </table>
                
                                <div class="observations">
                                    <strong>Observações:</strong><br>
                                    $observations
                                </div>
                
                                <table class="team-table" style="margin-top:10px">
                                    <thead>
                                        <tr><th>Função</th><th>Nome</th></tr>
                                    </thead>
                                    <tbody>
                                        $teamRows
                                    </tbody>
                                </table>
                
                                $signSection
                            </div>
                        </div>
                        """.trimIndent()
                }

                html = html.replace("{{MAINTENANCE_BLOCKS}}", maintenanceBlocks)

                val pdf = Utils.sendHtmlToPuppeteer(html, "http://localhost:3000/generate-pdf")

                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                    .body(pdf)

            }

            else -> {
                var html =
                    this::class.java.getResource("/templates/maintenance/conventional_grouped.html")!!.readText()

                val start = filtersRequest.startDate.atOffset(ZoneOffset.UTC)
                val end = filtersRequest.endDate.atOffset(ZoneOffset.UTC)

                val data = maintenanceRepository
                    .getGroupedConventionalMaintenances(start, end, filtersRequest.contractId)

                if (data.isEmpty()) {
                    throw IllegalArgumentException("Nenhum dado encontrado para os parâmetros fornecidos")
                }

                val root = data.first()
                val company = root["company"]!!
                val contract = root["contract"]!!
                val maintenances = root["maintenances"]!!

                val logoUrl = minioService.getPresignedObjectUrl(
                    Utils.getCurrentBucket(),
                    company["company_logo"].asText()
                )

                html = html
                    .replace("{{LOGO_IMAGE}}", logoUrl)
                    .replace("{{CONTRACT_NUMBER}}", contract["contract_number"].asText())
                    .replace("{{COMPANY_SOCIAL_REASON}}", company["social_reason"].asText())
                    .replace("{{COMPANY_CNPJ}}", company["company_cnpj"].asText())
                    .replace("{{COMPANY_ADDRESS}}", company["company_address"].asText())
                    .replace("{{COMPANY_PHONE}}", company["company_phone"].asText())
                    .replace("{{CONTRACTOR_SOCIAL_REASON}}", contract["contractor"].asText())
                    .replace("{{CONTRACTOR_CNPJ}}", contract["cnpj"].asText())
                    .replace("{{CONTRACTOR_ADDRESS}}", contract["address"].asText())
                    .replace("{{CONTRACTOR_PHONE}}", contract["phone"].asText())


                val maintenanceBlocks = maintenances.joinToString("\n") { m ->

                    // Início da execução
                    val start = Utils.convertToSaoPauloLocal(
                        Instant.parse(m.path("date_of_visit").asText())
                    )

                    // Fim da execução (fallback se não houver assinatura)
                    val end = if (m.hasNonNull("sign_date"))
                        Utils.convertToSaoPauloLocal(
                            Instant.parse(m.path("sign_date").asText())
                        )
                    else
                        start

                    // Datas formatadas (para exibição)
                    val dateOfVisit = start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    val signDate = end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

                    // Cálculo da duração
                    val duration = Duration.between(start, end)


                    // Evita duração negativa (defensivo)
                    val totalMinutes = maxOf(duration.toMinutes(), 0)

                    // Converte para horas + minutos
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60

                    // Texto final para relatório
                    val durationFormatted = "${hours}h ${minutes}min"


                    val streets = m["streets"]
                    val team = m["team"]
                    val total = m["total_by_maintenance"]

                    val streetRows = streets.mapIndexed { i, s ->
                        """
                            <tr>
                                <td>${i + 1}</td>
                                <td class="left">${s["address"].asText()}</td>
                                <td>${s["relay"].asText()}</td>
                                <td>${s["connection"].asText()}</td>
                                <td>${s["bulb"].asText()}</td>
                                <td>${s["sodium"].asText()}</td>
                                <td>${s["mercury"].asText()}</td>
                                <td>${s["power"].asText()}</td>
                                <td>${s["external_reactor"].asText()}</td>
                                <td>${s["internal_reactor"].asText()}</td>
                                <td>${s["relay_base"].asText()}</td>
                            </tr>
                        """.trimIndent()
                    }.joinToString("\n")

                    val observations = streets
                        .filter { it.has("comment") && !it["comment"].isNull }
                        .joinToString(" ") { it["comment"].asText() }

                    val teamRows = team.joinToString("\n") {
                        """
                            <tr>
                                <td>${it["role"].asText()}</td>
                                <td>${it["name"].asText()} ${it["last_name"].asText()}</td>
                            </tr>
                        """.trimIndent()
                    }

                    val signSection =
                        if (!m["signature_uri"].isNull) {
                            val signUrl = minioService.getPresignedObjectUrl(
                                Utils.getCurrentBucket(),
                                m["signature_uri"].asText()
                            )
                            """
                                <div class="signature">
                                    <img src="$signUrl">
                                    <div>Assinado em $signDate</div>
                                </div>
                            """.trimIndent()
                        } else ""

                    """
                        <div class="maintenance">
                            <div class="maintenance-header">
                                <div>Período: De $dateOfVisit às $signDate (Produtividade: $durationFormatted)</div>
                                <div>Tipo: ${m["type"].asText()} | Responsável pelo acompanhamento: ${m["responsible"].asText()}</div>
                            </div>
                
                            <div class="maintenance-body">
                                <table class="data-table">
                                    <thead>
                                        <tr>
                                            <th>Nº</th><th>Endereço</th><th>Relé</th><th>Conexão</th>
                                            <th>Lâmp.</th><th>Sódio</th><th>Merc.</th>
                                            <th>Pot.</th><th>Reator Ext.</th><th>Reator Int.</th><th>Base</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        $streetRows
                                    </tbody>
                                    <tfoot>
                                        <tr>
                                            <th colspan="2">Total por item</th>
                                            <th>${total["relay"].asText()}</th>
                                            <th>${total["connection"].asText()}</th>
                                            <th>${total["bulb"].asText()}</th>
                                            <th>${total["sodium"].asText()}</th>
                                            <th>${total["mercury"].asText()}</th>
                                            <th>N/A</th>
                                            <th>${total["external_reactor"].asText()}</th>
                                            <th>${total["internal_reactor"].asText()}</th>
                                            <th>${total["relay_base"].asText()}</th>
                                        </tr>
                                    </tfoot>
                                </table>
                
                                <div class="observations">
                                    <strong>Observações:</strong><br>
                                    $observations
                                </div>
                
                                <table class="data-table" style="margin-top:10px">
                                    <thead>
                                        <tr><th>Função</th><th>Nome</th></tr>
                                    </thead>
                                    <tbody>
                                        $teamRows
                                    </tbody>
                                </table>
                
                                $signSection
                            </div>
                        </div>
                    """.trimIndent()
                }

                val noGeneralTotal = root["generalTotal"]!!["values"]!!
                val generalTotal = """
                        <div class="maintenance">
                            <div class="maintenance-header">
                                <div>Total geral - Manutenções realizadas no período de ${startDate} a date</div>
                                <div>Tipo: tipo de manutenção</div>
                            </div>
                
                            <div class="maintenance-body">
                                <table class="data-table">
                                    <thead>
                                        <tr>
                                            <th>Relé</th><th>Conexão</th>
                                            <th>Lâmp.</th><th>Sódio</th><th>Merc.</th>
                                            <th>Pot.</th><th>Reator Ext.</th><th>Reator Int.</th><th>Base</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <td>${noGeneralTotal["relay"].asText()}</td>
                                            <td>${noGeneralTotal["connection"].asText()}</td>
                                            <td>${noGeneralTotal["bulb"].asText()}</td>
                                            <td>${noGeneralTotal["sodium"].asText()}</td>
                                            <td>${noGeneralTotal["mercury"].asText()}</td>
                                            <td>N/A</th>
                                            <td>${noGeneralTotal["external_reactor"].asText()}</td>
                                            <td>${noGeneralTotal["internal_reactor"].asText()}</td>
                                            <td>${noGeneralTotal["relay_base"].asText()}</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    """.trimIndent()

                html = html.replace("{{MAINTENANCE_BLOCKS}}", maintenanceBlocks)
                html = html.replace("{{GENERAL_TOTAL}}", generalTotal)

                println(html)

                val pdf = Utils.sendHtmlToPuppeteer(html, "http://localhost:3000/generate-pdf")

                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                    .body(pdf)
            }
        }

    }

    private fun generateGroupedInstallationReport(filtersRequest: ReportController.FiltersRequest): ResponseEntity<Any> {
        throw RuntimeException("Not implemented yet")
    }

    fun getContracts(): ResponseEntity<Any> {
        return ResponseEntity.ok().body(contractRepository.getContractsWithExecution(Utils.getCurrentTenantId()))
    }


}