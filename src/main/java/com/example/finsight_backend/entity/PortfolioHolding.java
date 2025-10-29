package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_holdings", indexes = {
        @Index(name = "idx_holding_portfolio", columnList = "portfolio_id"),
        @Index(name = "idx_holding_ticker", columnList = "ticker")
})
@Data
public class PortfolioHolding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    // We link to Company by ticker FK for simplicity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Double quantity; // number of shares

    @Column(name = "avg_price", nullable = false)
    private Double averagePrice; // average purchase price per share

    @Column(name = "min_threshold")
    private Double minThreshold; // optional lower price guardrail

    @Column(name = "max_threshold")
    private Double maxThreshold; // optional upper price guardrail

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
