package com.loanshark.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SendGridEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendGridEmailService.class);

    public void send(String email, String subject, String body) {
        if (email == null || email.isBlank()) {
            LOGGER.warn("Email not sent: recipient is null or blank.");
            return;
        }
        // TODO: Implement SendGrid email sending
        LOGGER.warn("SendGrid email sending not yet implemented. Would send to={}, subject={}", email, subject);
    }
}
