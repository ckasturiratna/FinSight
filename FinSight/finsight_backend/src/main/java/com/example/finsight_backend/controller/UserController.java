package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.LoginRequest;
import com.example.finsight_backend.dto.RegisterRequest;
import com.example.finsight_backend.dto.UserResponse;
import com.example.finsight_backend.dto.ResendOtpRequest;
import com.example.finsight_backend.dto.CompleteRegistrationRequest;
import com.example.finsight_backend.dto.ForgotPasswordRequest;
import com.example.finsight_backend.dto.ResetPasswordRequest;
import com.example.finsight_backend.dto.UpdateUserRequest;
import com.example.finsight_backend.dto.ChangePasswordConfirmRequest;
import com.example.finsight_backend.service.UserService;
import com.example.finsight_backend.service.OtpService;
import com.example.finsight_backend.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final OtpService otpService;
    private final EmailVerificationService emailVerificationService;
    
    // Simple in-memory cache to prevent duplicate requests within 5 seconds
    private final Map<String, LocalDateTime> recentRequests = new ConcurrentHashMap<>();

    //User Register - Initiate registration with OTP
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        
        // Check for duplicate requests within 5 seconds
        String email = request.getEmail();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastRequest = recentRequests.get(email);
        
        if (lastRequest != null && now.isBefore(lastRequest.plusSeconds(5))) {
            log.warn("Duplicate registration request for email: {} within 5 seconds", email);
            return ResponseEntity.ok("Verification code sent to your email");
        }
        
        // Store the request time
        recentRequests.put(email, now);
        
        // Clean up old entries (older than 1 minute)
        recentRequests.entrySet().removeIf(entry -> 
            now.isAfter(entry.getValue().plusMinutes(1))
        );
        
        userService.initiateRegistration(request);
        return ResponseEntity.ok("Verification code sent to your email");
    }

    //Verify OTP and complete registration
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody CompleteRegistrationRequest request) {
        log.info("OTP verification request received for email: {}", request.getEmail());
        
        try {
            // Verify OTP
            boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
            if (!isValid) {
                return ResponseEntity.badRequest().body("Invalid or expired OTP. Please try again or request a new code.");
            }

            // Complete registration
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .build();
                    
            UserResponse response = userService.completeRegistration(registerRequest);
            
            // Generate JWT token for automatic login
            String token = userService.generateTokenForUser(request.getEmail());
            
            // Return both user data and token
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", response);
            responseData.put("token", token);
            
            return new ResponseEntity<>(responseData, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Handle specific blocked account exceptions
            if (e.getMessage().contains("blocked") || e.getMessage().contains("too many")) {
                log.warn("Account blocked during OTP verification: {}", e.getMessage());
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            log.error("Error during OTP verification: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Verification failed. Please try again.");
        } catch (Exception e) {
            log.error("Unexpected error during OTP verification: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Verification failed. Please try again.");
        }
    }

    //Resend OTP
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Resend OTP request received for email: {}", request.getEmail());
        
        try {
            // Check if email is blocked
            if (otpService.isEmailBlocked(request.getEmail())) {
                // Clear the blocked status and send new OTP
                otpService.clearBlockedEmail(request.getEmail());
            }
            
            otpService.sendOtp(request.getEmail(), "User"); // We don't have the name here
            return ResponseEntity.ok("Verification code resent to your email");
        } catch (Exception e) {
            log.error("Error resending OTP: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to resend verification code. Please try again later.");
        }
    }

    //Debug endpoint to check verification status
    @GetMapping("/debug-verification/{email}")
    public ResponseEntity<?> debugVerification(@PathVariable String email) {
        log.info("Debug verification request for email: {}", email);
        try {
            return ResponseEntity.ok(otpService.getVerificationStatus(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    //Check if email needs verification
    @GetMapping("/check-verification/{email}")
    public ResponseEntity<?> checkVerification(@PathVariable String email) {
        log.info("Check verification request for email: {}", email);
        try {
            return ResponseEntity.ok(emailVerificationService.checkEmailVerificationStatus(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    //User Login
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        String token = userService.login(request);
        return ResponseEntity.ok(token);
    }

    // Authenticated: send OTP to email to change password
    @PostMapping("/change-password/initiate")
    public ResponseEntity<String> initiateChangePassword(Authentication authentication) {
        String email = authentication.getName();
        userService.initiateChangePassword(email);
        return ResponseEntity.ok("Verification code sent to your email");
    }

    // Authenticated: confirm OTP and set new password
    @PostMapping("/change-password/confirm")
    public ResponseEntity<String> confirmChangePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordConfirmRequest request
    ) {
        String email = authentication.getName();
        userService.confirmChangePassword(email, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    // Update current user (or by admin)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        String requesterEmail = authentication.getName();
        UserResponse updated = userService.updateUser(id, request, requesterEmail);
        return ResponseEntity.ok(updated);
    }

    // Delete current user (or by admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String requesterEmail = authentication.getName();
        userService.deleteUser(id, requesterEmail);
        return ResponseEntity.noContent().build();
    }

    //Forgot Password - Initiate password reset with OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request received for email: {}", request.getEmail());
        
        try {
            userService.initiateForgotPassword(request);
            return ResponseEntity.ok("Password reset code sent to your email");
        } catch (RuntimeException e) {
            log.warn("Forgot password failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during forgot password: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to send password reset code. Please try again later.");
        }
    }

    //Reset Password - Verify OTP and set new password
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received for email: {}", request.getEmail());
        
        try {
            userService.resetPassword(request);
            return ResponseEntity.ok("Password reset successfully. You can now login with your new password.");
        } catch (RuntimeException e) {
            log.warn("Reset password failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during password reset: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to reset password. Please try again later.");
        }
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User service is running");
    }
}
