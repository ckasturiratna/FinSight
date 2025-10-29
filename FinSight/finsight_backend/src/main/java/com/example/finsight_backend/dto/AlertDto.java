package com.example.finsight_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlertDto {
    private Long id;
    private String ticker;
    private String conditionType;
    private Double threshold;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}