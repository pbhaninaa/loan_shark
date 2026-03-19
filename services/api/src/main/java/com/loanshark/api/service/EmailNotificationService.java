package com.loanshark.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void send(String email, String subject, String body) {
        if (email == null || email.isBlank()) {
            LOGGER.warn("Email not sent: recipient is null or blank.");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.info("Email notification skipped because SMTP is not configured. Subject={}, to={}", subject, email);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setFrom("{{app.mail.from}}"); // This will use the property value
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            LOGGER.info("Email sent successfully to {}", email);
            System.out.println("Email sent successfully to " + email );
        } catch (Exception exception) {
            LOGGER.warn("Could not send email notification to {}", email, exception);
        }
    }
}