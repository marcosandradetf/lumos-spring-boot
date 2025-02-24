package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.controller.dto.MeasurementDTO;
import com.lumos.lumosspring.execution.service.PreMeasurementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;


@RestController
@RequestMapping("/api")
public class PreMeasurementController {
    private final PreMeasurementService preMeasurementService;

    public PreMeasurementController(PreMeasurementService preMeasurementService) {
        this.preMeasurementService = preMeasurementService;
    }

    @GetMapping("/mobile/execution/itens/{depositId}")
    public ResponseEntity<?> deposit(@PathVariable("depositId") Long depositId) {
        return preMeasurementService.getItems(depositId);
    }


    @PostMapping("/mobile/execution/insert-measurement")
    public ResponseEntity<?> saveMeasurement(@RequestBody MeasurementDTO measurementDTO, @RequestHeader("UUID") String userUUID) {
        return preMeasurementService.saveMeasurement(measurementDTO, userUUID);
    }

    @GetMapping("/execution/get-fields/{measurementId}")
    public ResponseEntity<?> getFields(@PathVariable long measurementId) {
        return preMeasurementService.getFields(measurementId);
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


}
