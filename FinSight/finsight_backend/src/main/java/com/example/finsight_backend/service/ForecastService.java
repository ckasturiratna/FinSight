package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.AccuracyMetricsDto;
import com.example.finsight_backend.dto.ForecastDto;
import com.example.finsight_backend.dto.ForecastLogDto;
import com.example.finsight_backend.entity.PredictionLog;
import com.example.finsight_backend.repository.PredictionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private final PredictionLogRepository predictionLogRepository;

    /**
     * Predicts the future price of a stock with confidence intervals.
     * NOTE: This is a placeholder implementation.
     */
    public ForecastDto predictWithCI(String ticker) {
        ForecastDto forecast = new ForecastDto();
        forecast.setTicker(ticker.toUpperCase());

        List<ForecastDto.ForecastPoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Generate 30 days of mock forecast data
        for (int i = 1; i <= 30; i++) {
            ForecastDto.ForecastPoint point = new ForecastDto.ForecastPoint();
            double basePrice = 150 + (i * 0.5); // Simulate a slight upward trend
            double confidenceMargin = 5 + (i * 0.2); // Simulate widening confidence

            point.setDate(today.plusDays(i));
            point.setMean(basePrice);
            point.setUpperBound(basePrice + confidenceMargin);
            point.setLowerBound(basePrice - confidenceMargin);
            points.add(point);
        }

        forecast.setForecastPoints(points);
        return forecast;
    }

    /**
     * Calculates the accuracy of past forecasts against actuals.
     */
    public AccuracyMetricsDto calculateAccuracyMetrics() {
        // Fetch all logs where an actual price is available
        List<PredictionLog> completedLogs = predictionLogRepository.findAllByActualPriceIsNotNull();

        if (completedLogs.isEmpty()) {
            AccuracyMetricsDto metrics = new AccuracyMetricsDto();
            metrics.setObservationCount(0);
            return metrics;
        }

        double absoluteErrorSum = 0.0;
        double percentageErrorSum = 0.0;
        double squaredErrorSum = 0.0;

        for (PredictionLog log : completedLogs) {
            double error = log.getPredictedPrice() - log.getActualPrice();
            absoluteErrorSum += Math.abs(error);
            percentageErrorSum += Math.abs(error) / log.getActualPrice();
            squaredErrorSum += error * error;
        }

        int n = completedLogs.size();
        AccuracyMetricsDto metrics = new AccuracyMetricsDto();
        metrics.setMeanAbsoluteError(absoluteErrorSum / n);
        metrics.setMeanAbsolutePercentageError(percentageErrorSum / n);
        metrics.setRootMeanSquaredError(Math.sqrt(squaredErrorSum / n));
        metrics.setObservationCount(n);

        return metrics;
    }

    /**
     * Retrieves all prediction logs to compare forecasts vs actuals.
     */
    public List<ForecastLogDto> getPredictionLogs() {
        return predictionLogRepository.findAll().stream()
                .map(this::toForecastLogDto)
                .collect(Collectors.toList());
    }

    private ForecastLogDto toForecastLogDto(PredictionLog log) {
        ForecastLogDto dto = new ForecastLogDto();
        dto.setDate(log.getDate());
        dto.setPredictedPrice(log.getPredictedPrice());
        dto.setActualPrice(log.getActualPrice());
        return dto;
    }
}
