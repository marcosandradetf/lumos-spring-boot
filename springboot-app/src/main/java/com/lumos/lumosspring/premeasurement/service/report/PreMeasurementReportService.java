package com.lumos.lumosspring.premeasurement.service.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lumos.lumosspring.premeasurement.repository.report.PreMeasurementReportRepository;
import com.lumos.lumosspring.s3.service.S3Service;
import com.lumos.lumosspring.util.Utils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
public class PreMeasurementReportService {
    private final PreMeasurementReportRepository repository;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;

    public PreMeasurementReportService(PreMeasurementReportRepository preMeasurementReportRepository, ObjectMapper objectMapper, S3Service s3Service) {
        this.repository = preMeasurementReportRepository;
        this.objectMapper = objectMapper;
        this.s3Service = s3Service;
    }

    public void getDataForReport(Long preMeasurementId) throws JsonProcessingException {
        String templateHtml;

        try (InputStream is = getClass()
                .getResourceAsStream("/templates/installation/data.html")) {
            if (is == null) {
                throw new IllegalArgumentException("Template not found");
            }

            templateHtml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new Utils.BusinessException(e.getMessage());
        }

        JsonNode root = objectMapper.readTree(repository.getDataForReport(preMeasurementId));
        JsonNode companyNode = root.get("company");
        JsonNode contractNode = root.get("contract");
        ArrayNode valuesArray = (ArrayNode) root.get("values");
        ArrayNode columnsArray = (ArrayNode) root.get("columns");
        ArrayNode teamArray = (ArrayNode) root.get("team");
        JsonNode streetsNode = root.get("streets");
        JsonNode streetSums = root.get("streetSums");
        JsonNode totalNode = root.get("total");

        String logoUri = companyNode.get("logoUri").asText();
        String companyLogoUrl = s3Service.getPresignedObjectUrl(Utils.INSTANCE.getCurrentBucket(), logoUri, 5 * 60);
        String teamRows = StreamSupport
                .stream(teamArray.spliterator(), false)
                .map(member -> {
                    String roleRaw = member.get("role").asText().toLowerCase();

                    String role = switch (roleRaw) {
                        case "electrician", "eletricista" -> "Eletricista";
                        case "driver", "motorista" -> "Motorista";
                        default -> "Executor";
                    };

                    String fullName = (
                            member.path("name").asText() + " " +
                                    member.path("last_name").asText()
                    ).trim();

                    return """
                            <tr>
                                <td>
                                    <p class="label">%s:</p>
                                    <p class="cell-text">%s:</p>
                                </td>
                            </tr>
                            """.formatted(role, fullName);
                }).collect(Collectors.joining("\n"));

        String valuesRows = IntStream
                .range(0, valuesArray.size())
                .mapToObj(index -> {
                    JsonNode line = valuesArray.get(index);

                    return """
                            <tr>
                                <td style="text-align: center;">%s</td>
                                <td style="text-align: left;">%s</td>
                                <td style="text-align: right;">%s</td>
                                <td style="text-align: right;">%s</td>
                                <td style="text-align: right;">%s</td>
                            </tr>
                            """.formatted(
                                    index + 1,
                                    line.path("description").asText(),
                                    Utils.INSTANCE.formatMoney(new BigDecimal(line.path("unit_price").asText())),
                                    line.path("quantity_executed").asText(),
                                    Utils.INSTANCE.formatMoney(new BigDecimal(line.path("total_price").asText()))
                            ).trim();
                }).collect(Collectors.joining("\n"));


    }

}
