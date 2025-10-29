package com.example.finsight_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PortfolioDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PortfolioHoldingDto> holdings;
}

