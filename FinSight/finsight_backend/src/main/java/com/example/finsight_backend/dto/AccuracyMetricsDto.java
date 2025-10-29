package com.example.finsight_backend.dto;

import lombok.Data;

@Data
public class AccuracyMetricsDto {
    private double meanAbsoluteError;       // MAE
    private double meanAbsolutePercentageError; // MAPE
    private double rootMeanSquaredError;      // RMSE
    private int observationCount;           // Number of data points used
}
