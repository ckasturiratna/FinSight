package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.AccuracyMetricsDto;
import com.example.finsight_backend.dto.ForecastDto;
import com.example.finsight_backend.dto.ForecastLogDto;
import com.example.finsight_backend.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecasts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ForecastController {

    private final ForecastService forecastService;

    @Operation(
            summary = "Get AI-powered forecast with confidence intervals",
            description = "Retrieves a 30-day price forecast for a given stock ticker, including upper and lower confidence bounds.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the forecast")
            }
    )
    @GetMapping("/{ticker}/ci")
    public ResponseEntity<ForecastDto> getForecastWithConfidenceIntervals(
            @Parameter(description = "The stock ticker symbol (e.g., 'AAPL')")
            @PathVariable String ticker) {
        ForecastDto forecast = forecastService.predictWithCI(ticker);
        return ResponseEntity.ok(forecast);
    }

    @Operation(
            summary = "Get forecast accuracy metrics",
            description = "Retrieves key metrics (MAE, MAPE, RMSE) evaluating the accuracy of past forecasts against actual market prices.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the accuracy metrics")
            }
    )
    @GetMapping("/accuracy")
    public ResponseEntity<AccuracyMetricsDto> getAccuracyMetrics() {
        AccuracyMetricsDto metrics = forecastService.calculateAccuracyMetrics();
        return ResponseEntity.ok(metrics);
    }

    @Operation(
            summary = "Get forecast vs. actuals history",
            description = "Retrieves a history of predicted prices vs. actual market prices for accuracy evaluation.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the prediction logs")
            }
    )
    @GetMapping("/logs")
    public ResponseEntity<List<ForecastLogDto>> getPredictionLogs() {
        return ResponseEntity.ok(forecastService.getPredictionLogs());
    }
}
