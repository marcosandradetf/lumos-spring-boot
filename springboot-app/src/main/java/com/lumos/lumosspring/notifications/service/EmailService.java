package com.lumos.lumosspring.notifications.service;

import com.lumos.lumosspring.notifications.entities.EmailConfig;
import com.lumos.lumosspring.notifications.model.EmailQueue;
import com.lumos.lumosspring.notifications.repository.EmailConfigRepository;
import com.lumos.lumosspring.config.SystemMailConfig;
import com.lumos.lumosspring.notifications.repository.EmailQueueRepository;
import com.lumos.lumosspring.util.Utils;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Properties;

@Service
public class EmailService {

    @Autowired
    private Resend resend;

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    @Value("${resend.from.email}")
    private String from;


    public CreateEmailResponse sendEmail(String to, String subject, String message) {
        if (to == null || subject == null || message == null) {
            throw new Utils.BusinessException("Missing required fields: to, subject, message");
        }

        try {
            var params = CreateEmailOptions.builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(message)
                    .build();

            return resend.emails().send(params);

        } catch (Exception e) {
            if (e.getMessage().contains("429") || e.toString().contains("Too Many Requests")) {
                System.out.println("⚠️ Limite do Resend atingido. Agendando para amanhã: " + to);

                // 1. Salva no banco com status PENDENTE
                emailQueueRepository.save(new EmailQueue(to, subject, message, "MANY_REQUESTS"));

                // 2. Avisa você via FCM (opcional, mas recomendado)
                // fcmService.sendNotification("Limite Atingido", "E-mail para " + to + " foi para a fila.");

                return null; // Ou retorne um objeto indicando que foi agendado
            }
            throw new RuntimeException("Erro ao enviar e-mail", e);
        }
    }


}
