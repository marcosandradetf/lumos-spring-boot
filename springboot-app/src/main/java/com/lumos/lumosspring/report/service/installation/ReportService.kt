package com.lumos.lumosspring.report.service.installation

import com.lumos.lumosspring.company.repository.CompanyRepository
import com.lumos.lumosspring.contract.repository.ContractRepository
import com.lumos.lumosspring.installation.repository.view.InstallationViewRepository
import com.lumos.lumosspring.maintenance.repository.MaintenanceRepository
import com.lumos.lumosspring.maintenance.service.MaintenanceService
import com.lumos.lumosspring.report.controller.installation.ExecutionReportController
import com.lumos.lumosspring.report.dto.execution.FiltersRequest
import com.lumos.lumosspring.s3.service.S3Service
import com.lumos.lumosspring.stock.materialsku.repository.MaterialReferenceRepository
import com.lumos.lumosspring.stock.materialsku.repository.TypeRepository
import com.lumos.lumosspring.util.CityUtils
import com.lumos.lumosspring.util.ExecutionStatus
import com.lumos.lumosspring.util.InstallationStatus
import com.lumos.lumosspring.util.Utils
import org.springframework.core.io.ClassPathResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class ReportService(
    private val maintenanceService: MaintenanceService,
    private val contractRepository: ContractRepository,
    private val installationReportService: InstallationReportService,
    private val installationViewRepository: InstallationViewRepository,
    private val s3Service: S3Service,
    private val templateEngine: SpringTemplateEngine,
    private val companyRepository: CompanyRepository,
    private val typeRepository: TypeRepository,
    private val materialReferenceRepository: MaterialReferenceRepository,
    private val maintenanceRepository: MaintenanceRepository
) {
    fun generateExecutionReport(filtersRequest: ExecutionReportController.FiltersRequest): ResponseEntity<Any> {
        return if (filtersRequest.scope == "MAINTENANCE") {
            generateMaintenanceReport(filtersRequest)
        } else {
            generateGroupedInstallationReport(filtersRequest)
        }
    }

    private fun generateMaintenanceReport(filtersRequest: ExecutionReportController.FiltersRequest): ResponseEntity<Any> {
        return if (filtersRequest.viewMode == "LIST") {

            val startUtc: OffsetDateTime =
                filtersRequest.startDate.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS)
            val endUtc: OffsetDateTime = filtersRequest.endDate.atOffset(ZoneOffset.UTC)
                .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)

            val response = maintenanceService.getGroupedMaintenances(
                filtersRequest.contractId,
                startUtc,
                endUtc,
                "%${filtersRequest.type}%"
            )

            ResponseEntity.ok().body(response)
        } else {
            maintenanceService.generateGroupedMaintenanceReport(filtersRequest)
        }
    }

    private fun generateGroupedInstallationReport(filtersRequest: ExecutionReportController.FiltersRequest): ResponseEntity<Any> {
        return if (filtersRequest.viewMode == "LIST") {

            val startUtc: OffsetDateTime =
                filtersRequest.startDate.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS)
            val endUtc: OffsetDateTime = filtersRequest.endDate.atOffset(ZoneOffset.UTC)
                .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)

            val response = installationReportService.getInstallationsData(
                filtersRequest.contractId,
                startUtc,
                endUtc
            )

            ResponseEntity.ok().body(response)
        } else if (filtersRequest.type == "data") {
            installationReportService.generateDataReport(filtersRequest)
        } else {
            installationReportService.generateDataReport(filtersRequest)
        }
    }

    fun getContracts(): ResponseEntity<Any> {
        return ResponseEntity.ok().body(contractRepository.getContractsWithExecution(Utils.getCurrentTenantId()))
    }

    fun generateOperationalReport(filtersRequest: FiltersRequest): ResponseEntity<Any> {
        val startUtc: OffsetDateTime = filtersRequest.startDate.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS)
        val endUtc: OffsetDateTime = filtersRequest.endDate.atOffset(ZoneOffset.UTC)
            .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)
        val company = companyRepository.getMainCompanyByTenantId(Utils.getCurrentTenantId()).orElseThrow()

        val installations = installationViewRepository.findFinishedMaterials(
            tenantId = Utils.getCurrentTenantId(),
            startedAt = startUtc,
            finishedAt = endUtc,
            contractIds = filtersRequest.contractIds,
            brands = filtersRequest.materialBrands.ifEmpty { null },
            types = filtersRequest.materialTypesIds.ifEmpty { null },
        )
        val maintenances = maintenanceRepository.findFinishedMaterials(
            tenantId = Utils.getCurrentTenantId(),
            startedAt = startUtc,
            finishedAt = endUtc,
            contractIds = filtersRequest.contractIds,
            brands = filtersRequest.materialBrands.ifEmpty { null },
            types = filtersRequest.materialTypesIds.ifEmpty { null },
        )


        val installationsByStatus = installations.groupBy { it.status }
        val maintenancesByStatus = maintenances.groupBy { it.status }

        val executions = (installationsByStatus.keys + maintenancesByStatus.keys)
            .distinct()
            .map { status ->

                val installationRows = installationsByStatus[status] ?: emptyList()
                val maintenanceRows = maintenancesByStatus[status] ?: emptyList()

                val cities = (installationRows + maintenanceRows)
                    .map { CityUtils.normalizeCityKey(it.contractor) }
                    .distinct()
                    .sorted()

                // -------- INSTALLATIONS --------
                val installationMaterials = installationRows
                    .groupBy { it.materialName }
                    .map { (materialName, rows) ->

                        val cityMap = rows
                            .groupBy { CityUtils.normalizeCityKey(it.contractor) }
                            .mapValues { (_, items) -> items.sumOf { it.quantity } }

                        ExecutionMaterial(
                            materialName = materialName,
                            materialBrand = rows.first().materialBrand,
                            cities = cityMap,
                            totalQuantity = rows.sumOf { it.quantity }
                        )
                    }

                val installationCityTotals = installationRows
                    .groupBy { CityUtils.normalizeCityKey(it.contractor) }
                    .mapValues { (_, rows) -> rows.sumOf { it.quantity } }

                // -------- MAINTENANCES --------
                val maintenanceMaterials = maintenanceRows
                    .groupBy { it.materialName }
                    .map { (materialName, rows) ->

                        val cityMap = rows
                            .groupBy { CityUtils.normalizeCityKey(it.contractor) }
                            .mapValues { (_, items) -> items.sumOf { it.quantity } }

                        ExecutionMaterial(
                            materialName = materialName,
                            materialBrand = rows.first().materialBrand,
                            cities = cityMap,
                            totalQuantity = rows.sumOf { it.quantity }
                        )
                    }

                val maintenanceCityTotals = maintenanceRows
                    .groupBy { CityUtils.normalizeCityKey(it.contractor) }
                    .mapValues { (_, rows) -> rows.sumOf { it.quantity } }

                ExecutionReport(
                    status = when (status) {
                        ExecutionStatus.FINISHED -> "FINALIZADO"
                        ExecutionStatus.IN_PROGRESS -> "EM ANDAMENTO"
                        else -> status
                    },
                    totalInstallations = installationMaterials.size,
                    totalMaintenances = maintenanceMaterials.size,
                    totalCities = cities.size,
                    totalSuppliers = (installationRows + maintenanceRows)
                        .map { it.materialBrand }
                        .distinct()
                        .size,
                    cities = cities,
                    installations = installationMaterials,
                    maintenances = maintenanceMaterials,
                    installationCityTotals = installationCityTotals,
                    maintenanceCityTotals = maintenanceCityTotals
                )
            }

        val companyLogo = s3Service.getPresignedObjectUrl(company.companyLogo)

        val logoResource = ClassPathResource("static/images/logo.png")
        val logoBytes = logoResource.inputStream.readAllBytes()
        val base64Logo = Base64.getEncoder().encodeToString(logoBytes)
        val context = Context()

        context.setVariable("executions", executions)
        context.setVariable("companyName", company.fantasyName)
        context.setVariable("companyLogo", companyLogo)
        context.setVariable("startPeriod", startUtc.format(DateTimeFormatter.ofPattern("dd/MM/yy")))
        context.setVariable("endPeriod", endUtc.format(DateTimeFormatter.ofPattern("dd/MM/yy")))
        context.setVariable("logoSystem", "data:image/png;base64,$base64Logo")

        val html = templateEngine.process(
            "execution/general",
            context
        )

        val pdf = Utils.sendHtmlToPuppeteer(html, "Relatório Analítico de Operações", filtersRequest.orientation)

        val responseHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            contentDisposition = ContentDisposition.attachment().filename("report.pdf").build()
        }

        return ResponseEntity.ok().headers(responseHeaders).body(pdf)
    }

    data class ExecutionMaterial(
        val materialName: String,
        val materialBrand: String?,
        val cities: Map<String, BigDecimal>,
        val totalQuantity: BigDecimal
    )

    data class ExecutionReport(
        val status: String,
        val totalInstallations: Int,
        val totalMaintenances: Int,
        val totalCities: Int,
        val totalSuppliers: Int,
        val cities: List<String>,
        val installations: List<ExecutionMaterial>,
        val maintenances: List<ExecutionMaterial>,
        val installationCityTotals: Map<String, BigDecimal>,
        val maintenanceCityTotals: Map<String, BigDecimal>
    )

}