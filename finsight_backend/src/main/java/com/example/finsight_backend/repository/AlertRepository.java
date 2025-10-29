package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.Alert;
import com.example.finsight_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUser(User user);
    Optional<Alert> findByIdAndUser_Id(Long alertId, Long userId);

    @Query("SELECT a FROM Alert a JOIN FETCH a.user WHERE a.active = true AND (a.cooldownExpiresAt IS NULL OR a.cooldownExpiresAt < CURRENT_TIMESTAMP)")
    List<Alert> findActiveAlertsReadyForEvaluation();
}