package com.lumos.lumosspring.installation.controller.premeasurement;

import com.lumos.lumosspring.installation.service.premeasurement.ViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InstallationViewController {
    private final ViewService viewService;

    public InstallationViewController(ViewService viewService) {
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
