package com.example.finsight_backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ForecastDto {
    private String ticker;
    private List<ForecastPoint> forecastPoints;

    @Data
    public static class ForecastPoint {
        private LocalDate date;
        private double mean;
        private double upperBound;
        private double lowerBound;
    }
}
