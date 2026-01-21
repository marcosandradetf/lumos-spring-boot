package com.lumos.lumosspring.premeasurement.controller.installation;

import com.lumos.lumosspring.premeasurement.service.installation.InstallationViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InstallationViewController {
    private final InstallationViewService viewService;

    public InstallationViewController(InstallationViewService viewService) {
        this.viewService = viewService;
    }

    @GetMapping("/mobile/v1/pre-measurement/installation/get-all/{status}")
    public ResponseEntity<?> getInstallations(@PathVariable String status) {
        return viewService.getInstallations(status);
    }

    @GetMapping("/mobile/v2/pre-measurement/installation/get-all/{status}/{teamId}")
    public ResponseEntity<?> getInstallations(@PathVariable String status, @PathVariable Long teamId) {
        return viewService.getInstallationsV2(status, teamId);
    }


}
