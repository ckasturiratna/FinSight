package com.example.finsight_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorPointDto {
    private long timestamp;
    private Double close;
    private Map<String, Double> overlays;
}
