package com.lumos.lumosspring.report.service

import org.springframework.http.*
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.OutputStreamWriter

@Service
class ReportService {
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

    fun generatePdfWithPuppeteer(htmlRequest: String, title: String): ResponseEntity<ByteArray?> {
        val client = WebClient.create("http://puppeteer-service:3000")

        val response = client.post()
        .uri("/generate-pdf")
        .setContentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf(
            "url" to "http://localhost:8080/report?id=123"
        ))
        .accept(MediaType.APPLICATION_PDF)
        .retrieve()
        .bodyToMono(ByteArray::class.java)
        .block()

        if (response == null) {
            throw RuntimeException("Erro ao gerar PDF com Puppeteer")
        }

        ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(response)
    }
}