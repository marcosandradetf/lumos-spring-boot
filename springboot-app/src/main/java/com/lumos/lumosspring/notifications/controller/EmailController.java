package com.lumos.lumosspring.notifications.controller;

import com.lumos.lumosspring.notifications.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService service;


    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        String subject = body.get("subject");
        String message = body.get("message");

        if (to == null || subject == null || message == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required fields: to, subject, message"));
        }

        var response = service.sendEmail(to, subject, message);

        return ResponseEntity.ok(Map.of("success", true, "id", response.getId()));
    }
}