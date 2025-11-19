package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.StockHistoryPointDto;
import com.example.finsight_backend.service.CandleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stocks/history")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StockHistoryController {

    private final CandleService candleService;

    @Operation(
            summary = "Get historical daily closes for a ticker",
            description = "Returns daily close prices from the given start date (defaults to last 90 days) up to today.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved history")
            }
    )
    @GetMapping("/{ticker}")
    public ResponseEntity<List<StockHistoryPointDto>> getHistory(
            @PathVariable String ticker,
            @Parameter(description = "Start date (inclusive) in yyyy-MM-dd format")
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate
    ) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = fromDate != null ? fromDate : today.minusDays(90);

        // Ensure the window is at least 1 day
        int days = (int) ChronoUnit.DAYS.between(start, today) + 1;
        if (days < 1) {
            days = 1;
        }

        Map<LocalDate, Double> closes = candleService.getDailyCloses(ticker, days);
        List<StockHistoryPointDto> payload = closes.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(start))
                .map(e -> {
                    StockHistoryPointDto dto = new StockHistoryPointDto();
                    dto.setDate(e.getKey());
                    dto.setClose(e.getValue());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(payload);
    }
}
