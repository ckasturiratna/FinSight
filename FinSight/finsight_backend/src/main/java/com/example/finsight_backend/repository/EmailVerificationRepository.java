package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndOtpAndIsUsedFalse(String email, String otp);

    Optional<EmailVerification> findByEmailAndIsUsedFalse(String email);

    Optional<EmailVerification> findByEmail(String email);

    @Modifying
    @Query("UPDATE EmailVerification ev SET ev.isUsed = true WHERE ev.email = :email")
    void markAsUsed(@Param("email") String email);

    @Modifying
    @Query("UPDATE EmailVerification ev SET ev.attempts = ev.attempts + 1 WHERE ev.email = :email AND ev.isUsed = false")
    void incrementAttempts(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.expiresAt < :now")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(ev) FROM EmailVerification ev WHERE ev.email = :email AND ev.createdAt > :since AND ev.isUsed = false")
    long countRecentVerifications(@Param("email") String email, @Param("since") LocalDateTime since);
}


