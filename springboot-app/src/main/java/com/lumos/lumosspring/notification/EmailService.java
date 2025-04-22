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

    @Autowired
    private SystemMailConfig systemMailConfig;

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

    public void sendNewPasswordForEmail(String toNome, String toEmail, String password) {
        String body = String.format("Olá, %s<br><br>" +
                        "Sua senha foi redefinida com sucesso no sistema Lumos. Para acessar sua conta, utilize a seguinte senha temporária:<br><br>" +
                        "<b>Nova Senha:</b> %s<br><br>" +
                        "Importante: Por questões de segurança, recomendamos que você altere sua senha assim que fizer login no sistema. Você pode fazer isso acessando a seção \"Alterar Senha\" nas configurações do seu usuário.<br><br>" +
                        "Se você tiver qualquer dúvida ou precisar de assistência, nossa equipe de suporte está à disposição para ajudá-lo.<br><br>" +
                        "Atenciosamente,<br>" +
                        "Equipe Thryon System<br>"
                ,toNome, password);
        sendSystemMailConfig(toEmail, body, "Lumos Thryon System - Nova Senha");
    }

    public void sendPasswordForEmail(String toNome, String toEmail, String password) {
        String body = String.format("Olá, %s,<br><br>" +
                "Sua conta foi criada com sucesso no sistema Lumos. Para acessar sua conta, utilize a seguinte senha temporária:<br><br>" +
                "<b>Senha:</b> %s<br><br>" +
                "Importante: Por questões de segurança, recomendamos que você altere sua senha assim que fizer login no sistema. Você pode fazer isso acessando a seção \"Alterar Senha\" nas configurações do seu usuário.<br><br>" +
                "Se você tiver qualquer dúvida ou precisar de assistência, nossa equipe de suporte está à disposição para ajudá-lo.<br><br>" +
                "Atenciosamente,<br>" +
                "Equipe Sistema Thryon<br>"
                ,toNome, password);
        sendSystemMailConfig(toEmail, body, "Lumos - Thryon System");
    }

    private void sendSystemMailConfig(String toEmail, String body, String subject) {
        try {
            JavaMailSender mailSender = systemMailConfig.getJavaMailSender();
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);
            helper.setFrom("no-reply@thryon.com.br");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            message.setContent(body, "text/html; charset=UTF-8"); // `true` permite HTML no corpo do e-mail.
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mails", e);
        }
    }


}
