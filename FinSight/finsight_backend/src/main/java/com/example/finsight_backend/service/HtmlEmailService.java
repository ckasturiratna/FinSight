package com.example.finsight_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@Service
public class HtmlEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    public HtmlEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("noreply@finsight.com", "FinSight Team");
            
            // Create Thymeleaf context
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            
            // Process the template
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    public void sendOtpVerificationEmail(String to, String firstName, String otp, int expiryMinutes) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "otp", otp,
            "expiryMinutes", expiryMinutes,
            "companyName", "FinSight"
        );
        
        sendHtmlEmail(to, "Your FinSight Verification Code", "otp-verification", variables);
    }

    public void sendPasswordResetEmail(String to, String firstName, String otp, int expiryMinutes) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "otp", otp,
            "expiryMinutes", expiryMinutes,
            "companyName", "FinSight"
        );
        
        sendHtmlEmail(to, "Your FinSight Password Reset Code", "password-reset", variables);
    }

    public void sendPriceAlertEmail(String to, String firstName, String message, String companyName, String currentPrice) {
        Map<String, Object> variables = Map.of(
            "firstName", firstName,
            "message", message,
            "companyName", companyName,
            "currentPrice", currentPrice
        );
        
        sendHtmlEmail(to, "FinSight Price Alert Triggered", "price-alert", variables);
    }
}
