package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "portfolio_history", uniqueConstraints = {
        @UniqueConstraint(name = "uq_portfolio_history_day", columnNames = {"portfolio_id", "snapshot_date"})
}, indexes = {
        @Index(name = "idx_history_portfolio", columnList = "portfolio_id"),
        @Index(name = "idx_history_date", columnList = "snapshot_date")
})
@Getter
@Setter
public class PortfolioHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "invested", nullable = false)
    private Double invested;

    @Column(name = "market_value", nullable = false)
    private Double marketValue;

    @Column(name = "pnl_abs", nullable = false)
    private Double pnlAbs;

    @Column(name = "pnl_pct", nullable = false)
    private Double pnlPct;

    @Column(name = "stale_count", nullable = false)
    private Integer staleCount;
}
