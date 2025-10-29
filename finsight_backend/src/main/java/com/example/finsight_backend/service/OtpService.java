package com.example.finsight_backend.service;

import com.example.finsight_backend.entity.EmailVerification;
import com.example.finsight_backend.repository.EmailVerificationRepository;
import com.example.finsight_backend.exception.AccountBlockedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final HtmlEmailService htmlEmailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_VERIFICATIONS_PER_HOUR = 3;
    private static final int BLOCK_DURATION_MINUTES = 10; // Auto-unblock after 10 minutes

    @Transactional
    public void sendOtp(String email, String firstName) {
        sendOtp(email, firstName, "registration");
    }

    @Transactional
    public void sendOtp(String email, String firstName, String type) {
        log.info("Sending OTP to email: {} for type: {}", email, type);

        // Check if user has exceeded verification attempts
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentVerifications = emailVerificationRepository.countRecentVerifications(email, oneHourAgo);

        log.info("Recent verifications for {}: {}", email, recentVerifications);

        if (recentVerifications >= MAX_VERIFICATIONS_PER_HOUR) {
            log.warn("Rate limit exceeded for email: {}", email);
            throw new RuntimeException("Too many verification attempts. Please try again later.");
        }

        // Generate OTP
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Handle verification record creation/update with proper unique constraint handling
        EmailVerification verification;
        
        try {
            // First try to find any existing record for this email
            Optional<EmailVerification> existingRecord = emailVerificationRepository.findByEmail(email);
            
            if (existingRecord.isPresent()) {
                log.info("Found existing verification record for email: {}, updating with new OTP", email);
                verification = existingRecord.get();
                verification.setOtp(otp);
                verification.setExpiresAt(expiresAt);
                verification.setAttempts(0);
                verification.setIsUsed(false);
            } else {
                log.info("Creating new verification record for email: {}", email);
                verification = EmailVerification.builder()
                        .email(email)
                        .otp(otp)
                        .expiresAt(expiresAt)
                        .build();
            }
            
            emailVerificationRepository.save(verification);
        } catch (Exception e) {
            // If there's a constraint violation, try to update existing record
            log.warn("Constraint violation for email: {}, attempting to update existing record", email);
            Optional<EmailVerification> existingRecord = emailVerificationRepository.findByEmail(email);
            if (existingRecord.isPresent()) {
                verification = existingRecord.get();
                verification.setOtp(otp);
                verification.setExpiresAt(expiresAt);
                verification.setAttempts(0);
                verification.setIsUsed(false);
                emailVerificationRepository.save(verification);
            } else {
                throw new RuntimeException("Failed to create or update verification record for email: " + email, e);
            }
        }
        log.info("Verification record saved for email: {}", email);

        // Send HTML email based on type
        if ("password_reset".equals(type)) {
            htmlEmailService.sendPasswordResetEmail(email, firstName, otp, OTP_EXPIRY_MINUTES);
        } else {
            htmlEmailService.sendOtpVerificationEmail(email, firstName, otp, OTP_EXPIRY_MINUTES);
        }

        log.info("OTP sent to email: {} for type: {}", email, type);
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        log.info("Verifying OTP for email: {} with OTP: {}", email, otp);

        // First, get the verification record for this email
        Optional<EmailVerification> verificationRecord = emailVerificationRepository
                .findByEmailAndIsUsedFalse(email);

        if (verificationRecord.isEmpty()) {
            log.warn("No active verification found for email: {}", email);
            return false;
        }

        EmailVerification verification = verificationRecord.get();
        log.info("Found verification record - attempts: {}, expires: {}, isUsed: {}",
                verification.getAttempts(), verification.getExpiresAt(), verification.getIsUsed());

        // Check if too many attempts FIRST
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            // Check if block has expired (10 minutes)
            LocalDateTime blockExpiry = verification.getCreatedAt().plusMinutes(BLOCK_DURATION_MINUTES);
            if (LocalDateTime.now().isAfter(blockExpiry)) {
                // Block has expired, reset attempts
                verification.setAttempts(0);
                emailVerificationRepository.save(verification);
                log.info("Block expired for email: {}, attempts reset to 0", email);
            } else {
                // Still blocked
                long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), blockExpiry).toMinutes();
                log.warn("OTP verification failed - too many attempts ({}) for email: {}, unblock in {} minutes",
                        verification.getAttempts(), email, remainingMinutes);
                throw new AccountBlockedException("Account blocked due to too many failed attempts. Please try again in " + remainingMinutes + " minutes or request a new verification code.", remainingMinutes);
            }
        }

        // Check if OTP is expired
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP verification failed - expired for email: {}", email);
            return false;
        }

        // Check if OTP matches
        if (!verification.getOtp().equals(otp)) {
            // Increment attempts for wrong OTP
            int newAttempts = verification.getAttempts() + 1;
            verification.setAttempts(newAttempts);
            emailVerificationRepository.save(verification);
            log.warn("OTP verification failed - wrong OTP for email: {} (attempts: {}/3)", email, newAttempts);
            return false;
        }

        // OTP is correct - mark as used
        verification.setIsUsed(true);
        emailVerificationRepository.save(verification);

        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    public void cleanupExpiredVerifications() {
        emailVerificationRepository.deleteExpiredVerifications(LocalDateTime.now());
        log.info("Cleaned up expired email verifications");
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupExpiredBlocks() {
        try {
            // Find all blocked verifications
            var blockedVerifications = emailVerificationRepository.findAll().stream()
                .filter(v -> !v.getIsUsed() && v.getAttempts() >= MAX_ATTEMPTS)
                .toList();

            int unblockedCount = 0;
            for (var verification : blockedVerifications) {
                LocalDateTime blockExpiry = verification.getCreatedAt().plusMinutes(BLOCK_DURATION_MINUTES);
                if (LocalDateTime.now().isAfter(blockExpiry)) {
                    verification.setAttempts(0);
                    emailVerificationRepository.save(verification);
                    unblockedCount++;
                }
            }

            if (unblockedCount > 0) {
                log.info("Auto-unblocked {} expired blocks", unblockedCount);
            }
        } catch (Exception e) {
            log.error("Error during block cleanup: {}", e.getMessage());
        }
    }

    public Object getVerificationStatus(String email) {
        Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndIsUsedFalse(email);
        if (verification.isPresent()) {
            EmailVerification v = verification.get();
            boolean isExpired = v.getExpiresAt().isBefore(LocalDateTime.now());
            boolean isBlocked = v.getAttempts() >= MAX_ATTEMPTS;

            // Check if block has expired
            if (isBlocked) {
                LocalDateTime blockExpiry = v.getCreatedAt().plusMinutes(BLOCK_DURATION_MINUTES);
                if (LocalDateTime.now().isAfter(blockExpiry)) {
                    // Block has expired, reset attempts
                    v.setAttempts(0);
                    emailVerificationRepository.save(v);
                    isBlocked = false;
                    log.info("Block expired for email: {}, attempts reset to 0", email);
                }
            }

            boolean canVerify = !isExpired && !isBlocked;
            long remainingBlockMinutes = 0;

            if (isBlocked) {
                LocalDateTime blockExpiry = v.getCreatedAt().plusMinutes(BLOCK_DURATION_MINUTES);
                remainingBlockMinutes = java.time.Duration.between(LocalDateTime.now(), blockExpiry).toMinutes();
            }

            return Map.of(
                "email", v.getEmail(),
                "attempts", v.getAttempts(),
                "maxAttempts", MAX_ATTEMPTS,
                "expiresAt", v.getExpiresAt(),
                "isUsed", v.getIsUsed(),
                "isExpired", isExpired,
                "isBlocked", isBlocked,
                "remainingBlockMinutes", remainingBlockMinutes,
                "canVerify", canVerify,
                "message", isBlocked ? "Account blocked. Please try again in " + remainingBlockMinutes + " minutes or request a new verification code." :
                          isExpired ? "OTP expired. Please request a new verification code." :
                          "OTP is valid and ready for verification."
            );
        }
        return Map.of("email", email, "status", "No active verification found", "canVerify", false);
    }

    public boolean isEmailBlocked(String email) {
        Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndIsUsedFalse(email);
        if (verification.isPresent()) {
            EmailVerification v = verification.get();
            boolean isBlocked = v.getAttempts() >= MAX_ATTEMPTS;

            if (isBlocked) {
                // Check if block has expired
                LocalDateTime blockExpiry = v.getCreatedAt().plusMinutes(BLOCK_DURATION_MINUTES);
                if (LocalDateTime.now().isAfter(blockExpiry)) {
                    // Block has expired, reset attempts
                    v.setAttempts(0);
                    emailVerificationRepository.save(v);
                    log.info("Block expired for email: {}, attempts reset to 0", email);
                    return false;
                }
            }

            return isBlocked;
        }
        return false;
    }

    public void clearBlockedEmail(String email) {
        Optional<EmailVerification> verification = emailVerificationRepository.findByEmailAndIsUsedFalse(email);
        if (verification.isPresent()) {
            EmailVerification v = verification.get();
            v.setAttempts(0); // Reset attempts
            v.setIsUsed(false); // Keep it as unused so sendOtp can find it
            emailVerificationRepository.save(v);
            log.info("Cleared blocked status for email: {}", email);
        }
    }

    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }
}
