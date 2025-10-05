package com.lumos.lumosspring.common.repository;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface InstallationReportRepository {
    List<Map<String, JsonNode>> getDataForReport(Long installationId);
    List<Map<String, JsonNode>> getDataPhotoReport(Long installationId);
}
