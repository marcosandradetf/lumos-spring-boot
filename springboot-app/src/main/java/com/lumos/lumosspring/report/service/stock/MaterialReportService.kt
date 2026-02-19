package com.lumos.lumosspring.report.service.stock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lumos.lumosspring.report.controller.stock.StockReportController
import com.lumos.lumosspring.report.dto.stock.TeamReport
import com.lumos.lumosspring.report.repository.stock.MaterialReportRepository
import com.lumos.lumosspring.s3.service.S3Service
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.InstallationStatus
import com.lumos.lumosspring.util.Utils
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@Service
class MaterialReportService(
    private val repository: MaterialReportRepository,
    private val s3Service: S3Service
) {
    private val mapper = jacksonObjectMapper()

    private fun getTeamReports(contractId: Long?, start: OffsetDateTime, end: OffsetDateTime): List<TeamReport> {
        val projections = repository.findTeamReportsJson(Utils.getCurrentTenantId(), contractId, start, end)
        return projections.map { proj ->
            TeamReport(
                teamName = proj.teamName,
                companyLogo = proj.companyLogo,
                fantasyName = proj.fantasyName,
                companyAddress = proj.companyAddress,
                companyCnpj = proj.companyCnpj,
                companyPhone = proj.companyPhone,
                installations = mapper.readValue(proj.installationsJson)
            )
        }
    }

    fun generateHtml(filters: StockReportController.FiltersRequest): ResponseEntity<Any> {
        val template = this::class.java.getResource("/templates/stock/stock-report-execution.html")!!.readText()

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val startUtc: OffsetDateTime = filters.startDate.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS)
        val endUtc: OffsetDateTime = filters.endDate.atOffset(ZoneOffset.UTC)
            .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)

        val localDateStart = Utils.convertToSaoPauloZoned(startUtc.toInstant())
        val localDateEnd = Utils.convertToSaoPauloZoned(endUtc.toInstant())
        val formattedDateStart = localDateStart.format(formatter)
        val formattedDateEnd = localDateEnd.format(formatter)

        val data = getTeamReports(filters.contractId, startUtc, endUtc)
        if (data.isEmpty() ||
            data.none { team ->
                team.installations.any { it.records.isNotEmpty() }
            }
        ) {
            throw Utils.BusinessException("Nenhum dado encontrado no período informado")
        }

        val executionsBlock = buildExecutionsBlock(data)

        var logoImage = data.first().companyLogo
        logoImage?.let { image ->
            logoImage = s3Service.getPresignedObjectUrl(Utils.getCurrentBucket(), image)
        }
        val html =  template
            .replace("{{TITLE_DOC}}", "Relatório de Saídas/Saldo de estoque por instalação")
            .replace("{{TITLE_PDF}}", "Saídas/Saldo de estoque por instalação")
            .replace("{{CONTRACT_NUMBER}}", "Período de $formattedDateStart à $formattedDateEnd")
            .replace("{{LOGO_IMAGE}}", logoImage ?: "")
            .replace("{{COMPANY_SOCIAL_REASON}}", data.first().fantasyName)
            .replace("{{COMPANY_CNPJ}}", data.first().companyCnpj)
            .replace("{{COMPANY_ADDRESS}}", data.first().companyAddress)
            .replace("{{COMPANY_PHONE}}", data.first().companyPhone)
            .replace("{{EXECUTIONS_BLOCK}}", executionsBlock)

        val pdf = Utils.sendHtmlToPuppeteer(html, "portrait")
        val responseHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            contentDisposition = ContentDisposition.attachment().filename("report.pdf").build()
        }

        return ResponseEntity.ok().headers(responseHeaders).body(pdf)
    }

    private fun buildExecutionsBlock(teamReports: List<TeamReport>): String {
        val sb = StringBuilder()

        teamReports.forEach { team ->
            sb.append("""
                <div class="page-content">
                    <div class="maintenance-header">
                        <span>Equipe: ${team.teamName}</span>
                        <span>Total de Instalações: ${team.installations.size}</span>
                    </div>
                    <div class="maintenance-body">
            """.trimIndent())

            team.installations.forEach { installation ->
                val translatedStatus = when (installation.status) {
                    ExecutionStatus.WAITING_STOCKIST -> InstallationStatus.WAITING_STOCKIST.translate("pt")
                    ExecutionStatus.WAITING_RESERVE_CONFIRMATION -> InstallationStatus.WAITING_RESERVE_CONFIRMATION.translate("pt")
                    ExecutionStatus.WAITING_COLLECT -> InstallationStatus.WAITING_COLLECT.translate("pt")
                    ExecutionStatus.AVAILABLE_EXECUTION -> InstallationStatus.AVAILABLE_EXECUTION.translate("pt")
                    ExecutionStatus.IN_PROGRESS -> InstallationStatus.IN_PROGRESS.translate("pt")
                    ExecutionStatus.FINISHED -> InstallationStatus.FINISHED.translate("pt")
                    else -> ExecutionStatus.WAITING_STOCKIST
                }

                val (bgColor, textColor, borderColor) = when (installation.status) {
                    ExecutionStatus.WAITING_STOCKIST -> Triple("bg-yellow-100", "text-yellow-800", "border-yellow-300")
                    ExecutionStatus.WAITING_RESERVE_CONFIRMATION -> Triple("bg-orange-100", "text-orange-800", "border-orange-300")
                    ExecutionStatus.WAITING_COLLECT -> Triple("bg-blue-100", "text-blue-800", "border-blue-300")
                    ExecutionStatus.AVAILABLE_EXECUTION -> Triple("bg-indigo-100", "text-indigo-800", "border-indigo-300")
                    ExecutionStatus.IN_PROGRESS -> Triple("bg-purple-100", "text-purple-800", "border-purple-300")
                    ExecutionStatus.FINISHED -> Triple("bg-green-100", "text-green-800", "border-green-300")
                    else -> Triple("bg-gray-100", "text-gray-800", "border-gray-300")
                }

                sb.append("""
                    <div class="bg-slate-100 border border-slate-300 border-l-4 border-l-slate-800 rounded p-3 my-3">
    
                        <div class="text-sm font-bold tracking-wide text-slate-900 mb-1">
                            ${installation.description.uppercase()}
                        </div>
        
                        <div class="text-xs text-slate-600">
                            Status:
                            <span class="ml-2 px-2 py-0.5 rounded-full text-xs font-semibold  $bgColor $textColor $borderColor">
                                $translatedStatus
                            </span>
                        </div>
        
                    </div>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th style="text-align:left;">Material</th>
                                <th style="text-align:right;"
                                    title="Quantidade liberada ou solicitada, dependendo do status.">
                                    Quantidade
                                </th>
                                <th style="text-align:right;"
                                    title="Quantidade executada em campo, pode ser definitivo ou não. Depende do status.">
                                    Qtde. Concluída
                                </th>
                                <th style="text-align:right;">Saldo</th>
                                <th style="text-align:right;"
                                    title="Quantidade atual em estoque no caminhão.">
                                    Qtde. no Caminhão
                                </th>
                                <th style="text-align:left;">Almoxarifado de retirada</th>
                                <th style="text-align:left;">Aberto por</th>
                                <th style="text-align:left;">Liberado por</th>
                                <th style="text-align:center;">Data de coleta</th>
                            </tr>
                        </thead>
                        <tbody>
                """.trimIndent())

                installation.records.forEach { record ->
                    val collectedAt = record.collectedAt?.let { Instant.parse(it) }
                    val localDate = collectedAt?.let { Utils.convertToSaoPauloZoned(it) }
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
                    val formattedDate = localDate?.format(formatter)

                    sb.append("""
                        <tr>
                            <td style="text-align:left;">${record.materialName}</td>
                            <td style="text-align:right;">${record.releasedQuantity}</td>
                            <td style="text-align:right;">${record.quantityCompleted}</td>
                            <td style="text-align:right;">${record.balance}</td>
                            <td style="text-align:right;">${record.truckQuantity}</td>
                            <td style="text-align:left;">${record.depositName}</td>
                            <td style="text-align:left;">${record.creator}</td>
                            <td style="text-align:left;">
                                ${record.responsible ?: "<span style='color:#888'>Não liberado</span>"}
                            </td>
                            <td style="text-align:center;">
                                ${formattedDate ?: "<span style='color:#888'>Não coletado</span>"}
                            </td>
                        </tr>
                    """.trimIndent())
                }

                sb.append("""
                        </tbody>
                    </table>
                    <br/>
                """.trimIndent())
            }

            sb.append("""
                    </div>
                </div>
            """.trimIndent())
        }

        return sb.toString()
    }
}
