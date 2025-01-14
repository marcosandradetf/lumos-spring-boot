package com.lumos.lumosspring.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Properties;

@Service
public class EmailService {

    @Autowired
    private EmailConfigRepository emailConfigRepository;

    public JavaMailSender getJavaMailSender() {
        Optional<EmailConfig> configOptional = emailConfigRepository.findById(1L); // Suponha que ID 1 seja o padrão.
        if (configOptional.isEmpty()) {
            throw new RuntimeException("Configuração de e-mail não encontrada");
        }

        EmailConfig config = configOptional.get();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", config.isAuth());
        props.put("mail.smtp.starttls.enable", config.isStarttls());

        return mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            JavaMailSender mailSender = getJavaMailSender();
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // `true` permite HTML no corpo do e-mail.
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mail", e);
        }
    }
}
