package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker", nullable = false)
    private Company company;

    @Column(name = "condition_type", nullable = false, length = 10)
    private String conditionType; // "GT" or "LT"

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "cooldown_expires_at")
    private LocalDateTime cooldownExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}