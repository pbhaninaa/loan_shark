package com.loanshark.api.service;

import com.sendgrid.SendGrid;
import com.sendgrid.Request;
import com.sendgrid.Method;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendGridEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendGridEmailService.class);

    @Value("${sendgrid.api-key}")
    private String apiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            LOGGER.warn("Email not sent: recipient is null or blank.");
            return;
        }

        try {
            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);

            LOGGER.info("Email sent successfully to {}", to);
            System.out.println("Email sent successfully to " + to);
        } catch (Exception e) {
            LOGGER.warn("Could not send email to {}", to, e);
            System.out.println("Could not send email to " + to);
        }
    }
}