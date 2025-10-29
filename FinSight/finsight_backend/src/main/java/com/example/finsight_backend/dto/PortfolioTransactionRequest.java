package com.example.finsight_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortfolioTransactionRequest {
    public enum Action { ADD, REMOVE }

    @NotBlank(message = "Ticker is required")
    private String ticker;

    @NotNull(message = "Action is required")
    private Action action;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0000001", inclusive = true, message = "Quantity must be greater than zero")
    private Double quantity;

    // Required for ADD, ignored for REMOVE
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    private Double price;
}

