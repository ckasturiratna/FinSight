package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.IndicatorResponseDto;
import com.example.finsight_backend.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/indicators")
@RequiredArgsConstructor
public class IndicatorController {

    private static final Set<String> ALLOWED_RESOLUTIONS = Set.of(
            "1", "5", "15", "30", "60", "D", "W", "M");

    private final IndicatorService indicatorService;

    @GetMapping("/{ticker}")
    @PreAuthorize("isAuthenticated()")
    public IndicatorResponseDto getIndicators(@PathVariable String ticker,
                                              @RequestParam(name = "resolution", defaultValue = "D") String resolution,
                                              @RequestParam(name = "count", defaultValue = "180") int count,
                                              @RequestParam(name = "sma", required = false) String sma,
                                              @RequestParam(name = "ema", required = false) String ema,
                                              @RequestParam(name = "rsi", required = false) String rsi) {
        String safeResolution = normaliseResolution(resolution);
        List<Integer> smaPeriods = parsePeriods(sma);
        List<Integer> emaPeriods = parsePeriods(ema);
        List<Integer> rsiPeriods = parsePeriods(rsi);
        return indicatorService.getIndicators(ticker, safeResolution, count, smaPeriods, emaPeriods, rsiPeriods);
    }

    private String normaliseResolution(String input) {
        if (input == null || input.isBlank()) {
            return "D";
        }
        String candidate = input.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_RESOLUTIONS.contains(candidate) ? candidate : "D";
    }

    private List<Integer> parsePeriods(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parseIntSafe)
                .filter(i -> i != null)
                .collect(Collectors.toList());
    }

    private Integer parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
