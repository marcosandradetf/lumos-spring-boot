package com.lumos.lumosspring.execution.controller;

import com.lumos.lumosspring.execution.dto.MeasurementValuesDTO;
import com.lumos.lumosspring.execution.entities.PreMeasurement;
import com.lumos.lumosspring.execution.repository.PreMeasurementRepository;
import com.lumos.lumosspring.execution.service.MeasurementService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/execution")
public class MeasurementController {
    private final MeasurementService measurementService;
    private final PreMeasurementRepository preMeasurementRepository;


    public MeasurementController(MeasurementService measurementService, PreMeasurementRepository preMeasurementRepository) {
        this.measurementService = measurementService;
        this.preMeasurementRepository = preMeasurementRepository;
    }

    @GetMapping("/get-fields/{measurementId}")
    public ResponseEntity<?> getFields(@PathVariable long measurementId) {
        return measurementService.getFields(measurementId);
    }

    @PostMapping("/pdf/generate")
    public ResponseEntity<byte[]> generatePdf(@RequestBody String htmlRequest) {
        Optional<PreMeasurement> calcBase = preMeasurementRepository.findById(1L);
        if (calcBase.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
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
                     <header class="header">
                          <p>Solutions Engenharia</p>
                          <p>RELATÓRIO DE EXECUÇÃO DE SERVIÇOS DE ILUMINAÇÃO PÚBLICA</p>
                        </header>
                        <div class="info">
                          <p>EMPRESA PRESTADORA: SCL SOLUTIONS ENGENHARIA</p>
                          <p>CONTRATO DE REFERÊNCIA: 5550125</p>
                          <p>CONTRATANTE: PREFEITURA MUNICIPAL DE BELO HORIZONTE</p>
                          <p>DATA: 03/08/2025</p>
                        </div>
                        <div class="description">
                          <h2>1. INTRODUÇÃO</h2>
                          <p>Este relatório tem como objetivo apresentar a execução dos serviços de instalação de iluminação
                            pública no município de Belo Horizonte, conforme solicitado pela Prefeitura Municipal. A seguir, serão
                            apresentadas as
                            tabelas contendo a base de cálculo de cada item e a relação das ruas atendidas com a quantidade de itens
                            instalados e valores correspondentes.
                          </p>
                        </div>
                        <h2>2. QUANTIDADE DE ITENS POR RUA</h2>
                    \s""";
            html = html.concat(htmlRequest);
            html = html.concat("<h2 class='titleReportBase'>3. BASE DE CÁLCULO DOS ITENS</h2>");
            html = html.concat(calcBase.get().getHtmlReport());
            html = html.concat("""
                      <div>
                        <h2>4. CONSIDERAÇÕES FINAIS</h2>
                        <p>
                          Solicitamos análise e aprovação deste relatório para a continuidade dos serviços.
                        </p>
                        <p>
                          Caso haja necessidade de ajustes ou revisões, favor informar para que possamos realizar as adequações necessárias.
                        </p>
                        <p>
                          Aguardamos o retorno sobre a aprovação integral ou parcial dos serviços executados.
                        </p>
                      </div>
                      <div class="assign">
                        <p>Atenciosamente,</p>
                        <p>Marcos Andrade</p>
                        <p>Cargo: Desenvolvedor</p>
                        <p>SCL Solutions</p>
                        <p>Contato: marcostfandrade@gmail.com</p>
                      </div>
                    </body>
                    </html>
                    """);

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

    @PostMapping("/save-pre-measurement-html-report/{preMeasurementId}")
    public ResponseEntity<?> saveHTMLReport(@RequestBody String html, @PathVariable Long preMeasurementId) {
        return measurementService.saveHtmlReport(html, preMeasurementId);
    }


}
