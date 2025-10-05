package com.lumos.lumosspring.premeasurement.controller.installation;

import com.lumos.lumosspring.premeasurement.dto.installation.InstallationRequest;
import com.lumos.lumosspring.premeasurement.service.installation.InstallationRegisterService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class InstallationRegisterController {
    private final InstallationRegisterService registerService;

    public InstallationRegisterController(InstallationRegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping(
            value = {"/mobile/v1/pre-measurement/installation/save-street"},
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public ResponseEntity<?> saveStreetInstallation(
            @RequestPart("photo") MultipartFile photo,
            @RequestPart("execution") InstallationRequest execution
    ) {
        return registerService.saveStreetInstallation(photo, execution);
    }


}
