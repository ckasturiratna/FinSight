package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.Notification;
import com.example.finsight_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime startDate, LocalDateTime endDate);

    List<Notification> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime startDate);

    List<Notification> findByUserAndCreatedAtBeforeOrderByCreatedAtDesc(User user, LocalDateTime endDate);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadForUser(Long userId);
}
