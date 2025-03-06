package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.controller.dto.MeasurementValuesDTO;
import com.lumos.lumosspring.execution.service.MeasurementService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/execution")
public class MeasurementController {
    private final MeasurementService measurementService;


    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/get-fields/{measurementId}")
    public ResponseEntity<?> getFields(@PathVariable long measurementId) {
        return measurementService.getFields(measurementId);
    }

    @PostMapping("/pdf/generate")
    public ResponseEntity<byte[]> generatePdf(@RequestBody String htmlRequest) {
        try {
            String html = """
                    <!DOCTYPE html>
                    <html lang="pt">
                    <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <title>Relatório de pré-medição</title>
                   \s
                        <style>
                            @page {
                                size: 35cm 29.7cm; /* Largura maior que um A4 padrão */
                                margin: 0;
                            }

                            body {
                                margin: 20px;
                                margin-top: 50px;
                                padding: 0;
                            }
                           \s
                            .report-base h3 {
                               background-color: #096cb8;
                               color: white;
                               text-align: center;
                               padding: 1em;
                               font-weight: bold;
                             }
                   \s
                             .report-base table {
                               border-collapse: collapse;
                               font-size: 0.7em;
                               font-family: sans-serif;
                               width: 100%;
                             }
                   \s
                             .report-base table th,
                             .report-base table td {
                               padding: 12px 15px;
                             }
                   \s
                             .report-base table tr {
                               border-bottom: 1px solid #dddddd;
                             }
                   \s
                             .report-base table tr:nth-of-type(even) {
                               background-color: #f3f3f3;
                             }
                   \s
                             .report-base table tr:last-of-type {
                               border-bottom: 2px solid #009879;
                             }
                   \s
                             .report-base table tr.active-row {
                               font-weight: bold;
                               color: #009879;
                             }
                   \s
                             .report-base table tr td input {
                               max-width: 120px;
                               background-color: inherit;
                               color: #242424;
                               padding: .15rem .5rem;
                               min-height: 25px;
                               border-radius: 4px;
                               outline: none;
                               border: none;
                               line-height: 1.15;
                               box-shadow: 0 10px 20px -18px;
                             }
                   \s
                             .report-base table tr td input:focus {
                               border-bottom: 2px solid #5b5fc7;
                               border-radius: 4px 4px 2px 2px;
                             }
                   \s
                             .report-base table tr td input:hover {
                               outline: 1px solid lightgrey;
                             }
                   \s
                             .report-base-total {
                               font-weight: bold;
                               background-color: #8be78b;
                             }
                   \s
                             .report-base-total-price{
                               width: 120px;
                             }
                   \s
                             .card-execution:hover {
                               border-bottom: 2px solid #5b5fc7;
                               border-radius: 4px;
                             }
                        </style>
                    </head>
                    <body>""";
            html = html.concat(htmlRequest);
            html = html.concat("</body>\n" +
                    "</html>");

            // Criar um OutputStream para armazenar o PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Criar o builder do OpenHTMLtoPDF e configurar a conversão
            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.withHtmlContent(html, "");  // Usa o HTML enviado na requisição
            builder.toStream(outputStream);           // Define o OutputStream como destino
            builder.run();                            // Executa a conversão

            // Obter os bytes do PDF gerado
            byte[] pdfContent = outputStream.toByteArray();

            // Configurar os cabeçalhos para resposta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio.pdf");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/save-pre-measurement-values/{preMeasurementId}")
    public ResponseEntity<?> saveMeasurementValues(@RequestBody Map<String, List<MeasurementValuesDTO>> valuesDTO, @PathVariable Long preMeasurementId) {
        return measurementService.saveMeasurementValues(valuesDTO, preMeasurementId);
    }


}
