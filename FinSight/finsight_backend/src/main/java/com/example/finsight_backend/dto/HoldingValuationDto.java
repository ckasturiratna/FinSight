package com.example.finsight_backend.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class HoldingValuationDto {
    private String ticker;
    private String name;
    private Double quantity;
    private Double averagePrice;
    private Double lastPrice;
    private Double invested;
    private Double marketValue;
    private Double pnlAbs;
    private Double pnlPct;
    private Instant priceAsOf;
    private boolean stale;
    private Double minThreshold;
    private Double maxThreshold;
}
