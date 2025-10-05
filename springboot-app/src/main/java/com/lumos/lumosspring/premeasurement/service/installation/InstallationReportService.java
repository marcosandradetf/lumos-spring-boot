package com.lumos.lumosspring.premeasurement.service.installation;

import org.springframework.stereotype.Service;

@Service
public class InstallationReportService {

//    fun generateDataReport(executionId: Long): ResponseEntity<ByteArray> {
//        var templateHtml = this::class.java.getResource("/templates/installation/data.html")!!.readText()
//
//        val data = jdbcInstallationRepository.getDataForReport(executionId)
//        val jsonData = data.first() // Pega o único resultado
//
//        val company = jsonData["company"]!!
//                val contract = jsonData["contract"]!!
//                val values = jsonData["values"]!!
//                val columns = jsonData["columns"]!!
//                val streets = jsonData["streets"]!!
//                val streetSums = jsonData["street_sums"]!!
//                val total = jsonData["total"]!!
//
//                val companyBucket =
//                company["bucket"]?.asText() ?: throw IllegalArgumentException("Company bucket does not exist")
//        val logoUri = company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo does not exist")
//        val companyLogoUrl = minioService.getPresignedObjectUrl(companyBucket, logoUri)
//
//        val replacements = mapOf(
//                "TITLE" to "RELATÓRIO DE INSTALAÇÃO DE LEDS - " + contract["contract_number"].asText(),
//                "CONTRACT_NUMBER" to contract["contract_number"].asText(),
//                "COMPANY_SOCIAL_REASON" to company["social_reason"].asText(),
//                "COMPANY_CNPJ" to company["company_cnpj"].asText(),
//                "COMPANY_ADDRESS" to company["company_address"].asText(),
//                "COMPANY_PHONE" to company["company_phone"].asText(),
//                "CONTRACTOR_SOCIAL_REASON" to contract["contractor"].asText(),
//                "CONTRACTOR_CNPJ" to contract["cnpj"].asText(),
//                "CONTRACTOR_ADDRESS" to contract["address"].asText(),
//                "CONTRACTOR_PHONE" to contract["phone"].asText(),
//                "LOGO_IMAGE" to companyLogoUrl,
//                "TOTAL_VALUE" to formatMoney(total["total_price"].asDouble()),
//                )
//
//        templateHtml = templateHtml.replacePlaceholders(replacements)
//
//        val valuesLines = values.mapIndexed { index, line ->
//                """
//                    <tr>
//                        <td style="text-align: center;">${index + 1}</td>
//                        <td style="text-align: left;">${line["description"].asText()}</td>
//                        <td style="text-align: right;">${formatMoney(line["unit_price"].asDouble())}</td>
//                        <td style="text-align: right;">${line["quantity_executed"].asText()}</td>
//                        <td style="text-align: right;">${formatMoney(line["total_price"].asDouble())}</td>
//                    </tr>
//                """.trimIndent()
//        }.joinToString("\n")
//
//        val columnsList = columns.map { it.asText() }
//
//        val streetColumnsHtml = columnsList.mapIndexed { index, columnName ->
//            if (index == 0)
//                "<th colspan=\"2\" style=\"text-align: left; font-weight: bold; min-width: 240px; max-width: 480px;\">$columnName</th>"
//            else
//                "<th style=\"text-align: center; font-weight: bold;width:40px;\">$columnName</th>"
//        }.joinToString("")
//
//
//        var dates: String? = null
//
//        val streetLinesHtml = streets.mapIndexed { index, line ->
//                val address = line[0].asText()
//            val lastPower = line[1].asText()
//            val items = line[2]  // ArrayNode
//
//            val date = Utils.convertToSaoPauloLocal(Instant.parse(line[3].asText()))
//                    .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
//
//            val supplier = line[4].asText()
//
//            if (index == 0) {
//                dates = "Execuções realizadas de $date"
//            } else if (index == streets.size() - 1) {
//                dates = "$dates à $date"
//            }
//
//            val quantityCells = items.joinToString("") { "<td style=\"text-align: right;\">${it.asText()}</td>" }
//
//            """
//                <tr>
//                    <td style="text-align: center;">${index + 1}</td>
//                    <td style="text-align: left; min-width: 240px; max-width: 480px; word-break: break-word;">$address</td>
//                    <td style="text-align: left;">$lastPower</td>
//                    $quantityCells
//                    <td style="text-align: right;">$date</td>
//                    <td style="text-align: left;">$supplier</td>
//                </tr>
//            """.trimIndent()
//        }.joinToString("\n")
//
//
//        val streetFooterHtml = streetSums.joinToString("") {
//            "<td style=\"text-align: right; font-weight: bold;\">${it.asText()}</td>"
//        }
//
//        templateHtml = templateHtml
//                .replace("{{VALUE_LINES}}", valuesLines)
//                .replace("{{STREET_COLUMNS}}", streetColumnsHtml)
//                .replace("{{STREET_LINES}}", streetLinesHtml)
//                .replace("{{STREET_FOOTER}}", streetFooterHtml)
//                .replace("{{COLUMN_LENGTH}}", (columnsList.size + 1).toString())
//                .replace("{{EXECUTION_DATE}}", if (dates != null) dates!! else "")
//
//        try {
//            val response = sendHtmlToPuppeteer(templateHtml)
//            val responseHeaders = HttpHeaders().apply {
//                contentType = MediaType.APPLICATION_PDF
//                contentDisposition = ContentDisposition.inline()
//                        .filename("RELATÓRIO DE INSTALAÇÃO DE LEDS - " + contract["contract_number"].asText() + ".pdf")
//                        .build()
//            }
//
//            return ResponseEntity.ok()
//                    .headers(responseHeaders)
//                    .body(response)
//        } catch (e: Exception) {
//            throw RuntimeException(e.message, e.cause)
//        }
//    }
//
//    fun generatePhotoReport(executionId: Long): ResponseEntity<ByteArray> {
//        var templateHtml = this::class.java.getResource("/templates/installation/photos.html")!!.readText()
//
//        val data = jdbcInstallationRepository.getDataPhotoReport(executionId)
//        val jsonData = data.first() // Pega o único resultado
//
//        val company = jsonData["company"]!!
//                val contract = jsonData["contract"]!!
//                val streets = jsonData["streets"]!!
//
//                val companyBucket =
//                company["bucket"]?.asText() ?: throw IllegalArgumentException("Company bucket does not exist")
//        val logoUri = company["company_logo"]?.asText() ?: throw IllegalArgumentException("Logo does not exist")
//        val companyLogoUrl = minioService.getPresignedObjectUrl(companyBucket, logoUri)
//
//        val streetLinesHtml = streets.joinToString("\n") { line ->
//                val photoUrl = minioService.getPresignedObjectUrl(companyBucket, line["execution_photo_uri"].asText())
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
//                    margin: 0;
//                    padding: 8px 12px;
//                    text-align: center;
//                    font-weight: bold;
//                    font-size: 12px;
//                    color: #054686;
//                    border-bottom: 1px solid #054686;
//                  ">
//                        ${line["address"].asText()}
//                    </p>
//
//                    <!-- Coordenadas -->
//                ${
//                if (line["latitude"].asText() != "null")
//                    """
//                    <p style="
//            margin: 0;
//            padding: 6px 12px;
//            text-align: center;
//            font-size: 11px;
//            color: #333;
//            border-bottom: 1px solid #ccc;
//            ">
//            Coordenadas - Latitude: ${line["latitude"].asText()}, Longitude: ${line["longitude"].asText()}
//                                </p>
//                    """.trimIndent()
//        else ""
//    }
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
//                "CONTRACT_NUMBER" to contract["contract_number"].asText(),
//                "COMPANY_SOCIAL_REASON" to company["social_reason"].asText(),
//                "COMPANY_CNPJ" to company["company_cnpj"].asText(),
//                "COMPANY_ADDRESS" to company["company_address"].asText(),
//                "COMPANY_PHONE" to company["company_phone"].asText(),
//                "CONTRACTOR_SOCIAL_REASON" to contract["contractor"].asText(),
//                "CONTRACTOR_CNPJ" to contract["cnpj"].asText(),
//                "CONTRACTOR_ADDRESS" to contract["address"].asText(),
//                "CONTRACTOR_PHONE" to contract["phone"].asText(),
//                "LOGO_IMAGE" to companyLogoUrl,
//                "PHOTOS" to streetLinesHtml,
//                )
//
//        templateHtml = templateHtml.replacePlaceholders(replacements)
//
//
//        try {
//            val response = sendHtmlToPuppeteer(templateHtml, "portrait")
//            val responseHeaders = HttpHeaders().apply {
//                contentType = MediaType.APPLICATION_PDF
//                contentDisposition = ContentDisposition.inline()
//                        .filename("RELATÓRIO FOTOGRÁFICO - CONTRATO Nº: " + contract["contract_number"].asText() + ".pdf")
//                        .build()
//            }
//
//            return ResponseEntity.ok()
//                    .headers(responseHeaders)
//                    .body(response)
//        } catch (e: Exception) {
//            throw RuntimeException(e.message, e.cause)
//        }
//    }
}
