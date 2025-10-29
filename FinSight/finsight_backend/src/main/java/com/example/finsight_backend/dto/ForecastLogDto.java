package com.example.finsight_backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ForecastLogDto {
    private LocalDate date;
    private double predictedPrice;
    private Double actualPrice;
}
