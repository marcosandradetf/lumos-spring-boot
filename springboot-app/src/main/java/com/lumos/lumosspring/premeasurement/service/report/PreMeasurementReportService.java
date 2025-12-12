package com.lumos.lumosspring.premeasurement.service.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumos.lumosspring.minio.service.MinioService;
import com.lumos.lumosspring.premeasurement.repository.report.PreMeasurementReportRepository;
import com.lumos.lumosspring.util.Utils;
import org.springframework.stereotype.Service;

@Service
public class PreMeasurementReportService {
    private final PreMeasurementReportRepository repository;
    private final ObjectMapper objectMapper;
    private final MinioService minioService;

    public PreMeasurementReportService(PreMeasurementReportRepository preMeasurementReportRepository, ObjectMapper objectMapper, MinioService minioService) {
        this.repository = preMeasurementReportRepository;
        this.objectMapper = objectMapper;
        this.minioService = minioService;
    }

    public void getDataForReport(Long preMeasurementId) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(repository.getDataForReport(preMeasurementId));
        JsonNode companyNode = root.get("company");
        JsonNode contractNode = root.get("contract");
        JsonNode valuesNode = root.get("values");
        JsonNode columnsNode = root.get("columns");
        JsonNode teamNode = root.get("team");
        JsonNode streetsNode = root.get("streets");
        JsonNode streetSums = root.get("streetSums");
        JsonNode totalNode = root.get("total");

        String logoUri = companyNode.get("logoUri").asText();
        String companyLogoUrl = minioService.getPresignedObjectUrl(Utils.INSTANCE.getCurrentBucket(), logoUri, 5 * 60);




    }

}
