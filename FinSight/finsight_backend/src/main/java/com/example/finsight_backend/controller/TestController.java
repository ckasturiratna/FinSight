package com.example.finsight_backend.controller;

import com.example.finsight_backend.service.EmailService;
import com.example.finsight_backend.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final EmailService emailService;
    private final OtpService otpService;

    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> protectedEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a protected endpoint");
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("Protected endpoint accessed");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a public endpoint");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestParam String email) {
        try {
            log.info("Sending test email to: {}", email);
            emailService.sendEmail(email, "Test Email", "This is a test email from FinSight backend.");
            log.info("Test email sent successfully to: {}", email);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Test email sent successfully");
            response.put("email", email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send test email to {}: {}", email, e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to send test email: " + e.getMessage());
            response.put("email", email);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/send-test-otp")
    public ResponseEntity<Map<String, String>> sendTestOtp(@RequestParam String email) {
        try {
            log.info("Sending test OTP to: {}", email);
            otpService.sendOtp(email, "Test User", "password_reset");
            log.info("Test OTP sent successfully to: {}", email);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Test OTP sent successfully");
            response.put("email", email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send test OTP to {}: {}", email, e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to send test OTP: " + e.getMessage());
            response.put("email", email);
            return ResponseEntity.badRequest().body(response);
        }
    }
}