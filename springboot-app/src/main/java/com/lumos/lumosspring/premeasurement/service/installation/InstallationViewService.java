package com.lumos.lumosspring.premeasurement.service.installation;

import com.lumos.lumosspring.premeasurement.repository.installation.InstallationViewRepository;
import com.lumos.lumosspring.util.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InstallationViewService {
    private final InstallationViewRepository viewRepository;

    public InstallationViewService(InstallationViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    public ResponseEntity<?> getInstallations(String status) {
        UUID userID = Utils.INSTANCE.getCurrentUserId();

        var executions = viewRepository.getInstallations(userID, status);

        return ResponseEntity.ok().body(executions);
    }
}
