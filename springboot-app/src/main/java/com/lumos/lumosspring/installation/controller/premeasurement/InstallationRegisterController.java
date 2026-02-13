package com.lumos.lumosspring.installation.controller.premeasurement;

import com.lumos.lumosspring.installation.dto.premeasurement.InstallationRequest;
import com.lumos.lumosspring.installation.dto.premeasurement.InstallationStreetRequest;
import com.lumos.lumosspring.installation.service.premeasurement.RegisterService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class InstallationRegisterController {
    private final RegisterService registerService;

    public InstallationRegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping(
            value = {"/mobile/v1/pre-measurement/installation/save-street"},
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public ResponseEntity<?> saveStreetInstallation(
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart("installationStreet") InstallationStreetRequest installationStreet
    ) {
        return registerService.saveStreetInstallation(photo, installationStreet);
    }

    @PostMapping(
            value = {"/mobile/v1/pre-measurement/installation/save-installation"},
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public ResponseEntity<?> saveInstallation(
            @RequestPart(value = "signature", required = false) MultipartFile signature,
            @RequestPart("installation") InstallationRequest installationReq
    ) {
        return registerService.saveInstallation(signature, installationReq);
    }


}
