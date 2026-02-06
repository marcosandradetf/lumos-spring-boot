package com.lumos.lumosspring.report.service

import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.executionreport.installation.InstallationReportService
import com.lumos.lumosspring.maintenance.service.MaintenanceService

import com.lumos.lumosspring.report.controller.ReportController
import com.lumos.lumosspring.util.Utils
import org.springframework.http.*
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.ZoneOffset

@Service
class ReportService(
    private val maintenanceService: MaintenanceService,
    private val contractRepository: ContractRepository,
    private val installationReportService: InstallationReportService,
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

            val response = maintenanceService.getGroupedMaintenances(
                filtersRequest.contractId,
                start,
                end,
                "%${filtersRequest.type}%"
            )

            ResponseEntity.ok().body(response)
        } else {
            maintenanceService.generateGroupedMaintenanceReport(filtersRequest)
        }
    }

    private fun generateGroupedInstallationReport(filtersRequest: ReportController.FiltersRequest): ResponseEntity<Any> {
        return if (filtersRequest.viewMode == "LIST") {

            val start = filtersRequest.startDate.atOffset(ZoneOffset.UTC)
            val end = filtersRequest.endDate.atOffset(ZoneOffset.UTC)

            val response = installationReportService.getInstallationsData(
                filtersRequest.contractId,
                start,
                end
            )

            ResponseEntity.ok().body(response)
        } else if(filtersRequest.type == "data") {
            installationReportService.generateDataReport(filtersRequest)
        } else {
            installationReportService.generateDataReport(filtersRequest)
        }
    }

    fun getContracts(): ResponseEntity<Any> {
        return ResponseEntity.ok().body(contractRepository.getContractsWithExecution(Utils.getCurrentTenantId()))
    }


}