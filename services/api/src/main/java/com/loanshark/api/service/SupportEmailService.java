package com.loanshark.api.service;


import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SupportEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    // Default support email
    private static final String SUPPORT_EMAIL_FROM = "support@loanshark.com";

    public SupportEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    /**
     * Send a support email with HTML and inline image signature.
     *
     * @param to      recipient email
     * @param subject email subject
     * @param body    email body (plain text, will be wrapped in HTML)
     */
    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            LOGGER.warn("Support email not sent: recipient is null or blank.");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.info("Support email skipped because SMTP is not configured. Subject={}, to={}", subject, to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(SUPPORT_EMAIL_FROM);
            helper.setSubject(subject);

            // HTML body with signature image
            String htmlBody = "<html><body style='font-family: Arial, sans-serif;'>"
                    + "<p>" + body + "</p>"
                    + getSupportSignatureHtml()
                    + "</body></html>";

            helper.setText(htmlBody, true); // HTML enabled

            // Inline image signature
            ClassPathResource signatureImage = new ClassPathResource("signature.png");
            helper.addInline("signatureImage", signatureImage);

            mailSender.send(message);

            LOGGER.info("Support email sent successfully to {}", to);
            System.out.println("Support email sent successfully to " + to);

        } catch (Exception e) {
            LOGGER.warn("Could not send support email to {}", to, e);
        }
    }

    // HTML signature with inline image
    private String getSupportSignatureHtml() {
        return "<br><br>"
                + "<hr>"
                + "<p>Best regards,<br><strong>Loan Shark Support Team</strong></p>"
                + "<img src='cid:signatureImage' alt='Signature' style='width:100%; max-width:600px; height:auto;' />";
    }
}