package com.example.finsight_backend.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class PortfolioValuationDto {
    private Long portfolioId;
    private String currency = "USD";
    private Instant updatedAt;

    @Data
    public static class Totals {
        private Double invested;
        private Double marketValue;
        private Double pnlAbs;
        private Double pnlPct;
        private Integer staleCount;
    }

    private Totals totals;
    private List<HoldingValuationDto> holdings;
}

