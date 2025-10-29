package com.example.finsight_backend.service;

import com.example.finsight_backend.repository.EmailVerificationRepository;
import com.example.finsight_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    /**
     * Check if an email needs verification
     * @param email The email to check
     * @return Map with verification status
     */
    public Map<String, Object> checkEmailVerificationStatus(String email) {
        // Check if user already exists
        boolean userExists = userRepository.existsByEmail(email);
        
        if (userExists) {
            return Map.of(
                "email", email,
                "needsVerification", false,
                "message", "Email is already verified and account exists",
                "canLogin", true
            );
        }

        // Check if there's an active verification
        var verification = emailVerificationRepository.findByEmailAndIsUsedFalse(email);
        
        if (verification.isPresent()) {
            var v = verification.get();
            boolean isExpired = v.getExpiresAt().isBefore(java.time.LocalDateTime.now());
            boolean isBlocked = v.getAttempts() >= 3;
            
            return Map.of(
                "email", email,
                "needsVerification", true,
                "isExpired", isExpired,
                "isBlocked", isBlocked,
                "attempts", v.getAttempts(),
                "message", isBlocked ? "Account blocked. Please request a new verification code." :
                          isExpired ? "Previous verification expired. Please request a new code." :
                          "Verification code is active and ready for use.",
                "canLogin", false
            );
        }

        return Map.of(
            "email", email,
            "needsVerification", true,
            "message", "No verification found. Please register first.",
            "canLogin", false
        );
    }
}


