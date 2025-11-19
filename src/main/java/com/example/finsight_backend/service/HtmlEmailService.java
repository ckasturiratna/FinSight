package com.example.finsight_backend.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class HtmlEmailService {

    private final MailjetEmailService mailjetEmailService;
    private final TemplateEngine templateEngine;

    public HtmlEmailService(MailjetEmailService mailjetEmailService, TemplateEngine templateEngine) {
        this.mailjetEmailService = mailjetEmailService;
        this.templateEngine = templateEngine;
    }

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        // Create Thymeleaf context
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }

        // Process the template
        String htmlContent = templateEngine.process(templateName, context);
        String textContent = htmlContent.replaceAll("<[^>]*>", "").trim();

        mailjetEmailService.sendEmail(to, subject, textContent, htmlContent);
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
