package com.example.finsight_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PredictionResponse {
    private String ticker;

    @JsonProperty("predicted_price")
    private double predicted_price;
}
