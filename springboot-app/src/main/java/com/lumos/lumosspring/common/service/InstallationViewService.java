package com.lumos.lumosspring.common.service;

import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface InstallationViewService {
    ResponseEntity<?> getPendingInstallations(UUID operatorUUID);
    ResponseEntity<?> getFinishedInstallations(Long companyId);
    ResponseEntity<?> getInProgressInstallations(Long companyId);
}
