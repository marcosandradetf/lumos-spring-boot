package com.lumos.lumosspring.report.service

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class ReportService {
    fun generatePdf(htmlRequest: String, title: String): ResponseEntity<ByteArray?> {
        try {
            var html = """
                     <!DOCTYPE html>
                     <html lang="pt">
                     <head>
                         <meta charset="UTF-8" />
                         <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                         <title>$title</title>
                     
                         <style>
                              @page {
                                size: 35cm 29.7cm;
                                /* Largura maior que um A4 padrão */
                                margin: 0;
                              }
                              .header {
                                width: 100%;
                                padding: 10px;
                                margin-bottom: 20px;
                                border-bottom: 2px solid darkorange;
                                font-family: sans-serif;
                              }
                              .header p {
                                font-weight: bold;
                                font-size: 0.9em;
                                 display: inline-block;
                                 width: 45%;
                                 margin: 5px;
                                 vertical-align: top;
                              }
                              .report,
                              .report-base {
                                padding: 0 50px 0 50px;
                              }
                              .titleReportBase {
                                page-break-before: always;
                                margin-top: 20px;
                              }
                              body {
                                margin: 0;
                                padding: 0;
                              }
                              table {
                                border-collapse: collapse;
                                font-size: 0.7em;
                                font-family: sans-serif;
                                width: 100%;
                                border-radius: 10px;
                              }
                              table th,
                              table td {
                                padding: 12px 15px;
                              }
                              table tr {
                                border-bottom: 1px solid #dddddd;
                              }
                              table tr:nth-child(even) {
                                   background-color: #f3f3f3;
                              }
                              .report-total-sum {
                                background-color: #108cc8;
                                color: white;
                                font-weight: bold;
                              }
                              .report-total-price {
                                font-family: Georgia, serif;
                                font-size: 1em;
                                margin-top: 10px;
                                width: fit-content;
                              }
                              .report-header {
                                background-color: #096cb8;
                                color: white;
                                text-align: left;
                              }
                              table {
                                border: 2px solid #dddddd;
                              }
                              .report-base-header {
                                background-color: #096cb8;
                                color: white;
                                text-align: left;
                              }
                              .report-base-total {
                                font-weight: bold;
                                background-color: #8be78b;
                              }
                              .report-base-total-price {
                                width: 120px;
                              }
                              div {
                                padding: 20px 0 20px 0;
                              }
                              p {
                                font-family: sans-serif;
                                margin: 0;
                                padding: 0 20px 0 20px;
                              }
                              h2 {
                                font-size: 1.1em;
                                font-family: sans-serif;
                                margin: 0;
                                padding: 0 20px 10px 20px;
                              }
                              .assign{
                                font-size: 0.8em;
                                margin-top: auto;
                              }
                            </style>
                     </head>
                     <body>
                      
                      """.trimIndent()
            html += htmlRequest
            html = "$html</body></html>"

            // Criar um OutputStream para armazenar o PDF
            val outputStream = ByteArrayOutputStream()

            // Criar o builder do OpenHTMLtoPDF e configurar a conversão
            val builder = PdfRendererBuilder()

            builder.withHtmlContent(html, "") // Usa o HTML enviado na requisição
            builder.toStream(outputStream) // Define o OutputStream como destino
            builder.run() // Executa a conversão

            // Obter os bytes do PDF gerado
            val pdfContent = outputStream.toByteArray()

            // Configurar os cabeçalhos para resposta HTTP
            val headers = HttpHeaders()
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-$title.pdf")
            headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf")


            return ResponseEntity(pdfContent, headers, HttpStatus.OK)
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }
}