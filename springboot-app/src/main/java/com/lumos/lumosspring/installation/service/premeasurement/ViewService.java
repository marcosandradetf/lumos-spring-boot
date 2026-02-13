package com.lumos.lumosspring.installation.service.premeasurement;

import com.lumos.lumosspring.installation.repository.premeasurement.ViewRepository;
import com.lumos.lumosspring.util.Utils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ViewService {
    private final ViewRepository viewRepository;

    public ViewService(ViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    @Cacheable(
            value = "getPreMeasurementInstallations",
            key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
    )
    public ResponseEntity<?> getInstallations(String status) {
        UUID userID = Utils.getCurrentUserId();
        var executions = viewRepository.getInstallations(userID, status, null);

        return ResponseEntity.ok().body(executions);
    }

    @Cacheable(
            value = "getPreMeasurementInstallations",
            key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
    )
    public ResponseEntity<?> getInstallationsV2(String status, Long teamId) {
        var executions = viewRepository.getInstallations(null, status, teamId);

        return ResponseEntity.ok().body(executions);
    }
}
