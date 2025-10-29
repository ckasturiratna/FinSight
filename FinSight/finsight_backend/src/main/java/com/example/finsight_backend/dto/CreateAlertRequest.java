package com.example.finsight_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAlertRequest {
    @NotBlank(message = "Ticker is required")
    private String ticker;

    @NotBlank(message = "Condition type is required (e.g., 'GT' or 'LT')")
    private String conditionType;

    @NotNull(message = "Threshold price is required")
    private Double threshold;
}