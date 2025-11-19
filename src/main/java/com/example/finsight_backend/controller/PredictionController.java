package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.PredictionResponse;
import com.example.finsight_backend.service.StockPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PredictionController {

    private final StockPredictionService stockPredictionService;

    @Operation(
            summary = "Get AI-powered price prediction",
            description = "Retrieves the predicted price for the provided stock ticker from the AI predictor service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the prediction")
            }
    )
    @GetMapping("/{ticker}")
    public ResponseEntity<PredictionResponse> getPrediction(@PathVariable String ticker) {
        return ResponseEntity.ok(stockPredictionService.predict(ticker));
    }
}
