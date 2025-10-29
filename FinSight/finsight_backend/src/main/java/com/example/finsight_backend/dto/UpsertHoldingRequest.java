package com.example.finsight_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpsertHoldingRequest {
    @NotBlank(message = "Ticker is required")
    private String ticker;

    @DecimalMin(value = "0.0", inclusive = true, message = "Quantity must be non-negative")
    private Double quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Average price must be non-negative")
    private Double averagePrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum threshold must be non-negative")
    private Double minThreshold;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum threshold must be non-negative")
    private Double maxThreshold;
}
