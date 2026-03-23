package com.lumos.lumosspring.notifications.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.svix.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/email")
public class EmailWebhookController {

    @Value("${resend.webhook.secret:}")
    private String webhookSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "svix-id", required = false) String svixId,
            @RequestHeader(value = "svix-timestamp", required = false) String svixTimestamp,
            @RequestHeader(value = "svix-signature", required = false) String svixSignature) {

        if (svixId == null || svixTimestamp == null || svixSignature == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cabeçalhos do webhook ausentes"));
        }

        if (webhookSecret == null || webhookSecret.isEmpty()) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Segredo do webhook não configurado"));
        }

        try {
            Webhook wh = new Webhook(webhookSecret);
            java.net.http.HttpHeaders headers = java.net.http.HttpHeaders.of(
                    Map.of(
                            "svix-id", List.of(svixId),
                            "svix-timestamp", List.of(svixTimestamp),
                            "svix-signature", List.of(svixSignature)
                    ),
                    (name, value) -> true // Filtro padrão para aceitar todos
            );

            wh.verify(payload, headers);

            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("type");

            System.out.println("Evento de webhook recebido: " + eventType);

            switch (eventType) {
                case "email.received" -> {
                    Map<String, Object> data = (Map<String, Object>) event.get("data");
                    System.out.println("Novo e-mail de: " + data.get("from"));
                }
                case "email.delivered" -> {
                    Map<String, Object> data = (Map<String, Object>) event.get("data");
                    System.out.println("E-mail entregue: " + data.get("email_id"));
                }
                case "email.bounced" -> {
                    Map<String, Object> data = (Map<String, Object>) event.get("data");
                    System.out.println("E-mail rejeitado (bounced): " + data.get("email_id"));
                }
                case "email.failed" -> {
                    Map<String, Object> data = (Map<String, Object>) event.get("data");
                    System.out.println("Falha no processamento do e-mail: " + data.get("email_id"));
                }
            }

            return ResponseEntity.ok(Map.of("received", true, "type", eventType));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Falha na verificação: " + e.getMessage()));
        }
    }
}