package com.example.finsight_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StockPriceDto {
    private String ticker;
    private Double currentPrice;
    private Long volume;
    private Double percentChange;
    private Double high;
    private Double low;
    private LocalDateTime timestamp;
}