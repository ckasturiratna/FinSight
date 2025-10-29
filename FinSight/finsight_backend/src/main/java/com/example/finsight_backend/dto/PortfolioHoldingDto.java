package com.example.finsight_backend.dto;

import lombok.Data;

@Data
public class PortfolioHoldingDto {
    private Long id;
    private String ticker;
    private String name;
    private Double quantity;
    private Double averagePrice;
    private Double minThreshold;
    private Double maxThreshold;
}
