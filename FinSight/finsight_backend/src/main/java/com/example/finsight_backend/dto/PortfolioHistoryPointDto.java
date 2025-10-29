package com.example.finsight_backend.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class PortfolioHistoryPointDto {
    private Long id;
    private LocalDate snapshotDate;
    private Instant capturedAt;
    private Double invested;
    private Double marketValue;
    private Double pnlAbs;
    private Double pnlPct;
    private Integer staleCount;
}
