package com.lumos.lumosspring.common.service;

import org.springframework.http.ResponseEntity;

public interface InstallationReportService {
    ResponseEntity<byte[]> generateDataReport(Long executionId);
    ResponseEntity<byte[]> generatePhotoReport(Long executionId);
}
