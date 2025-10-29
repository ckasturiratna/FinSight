package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.IndicatorDefinitionDto;
import com.example.finsight_backend.dto.IndicatorPointDto;
import com.example.finsight_backend.dto.IndicatorResponseDto;
import com.example.finsight_backend.repository.CompanyRepository;
import com.example.finsight_backend.util.IndicatorCalculator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicatorService {

    private static final int DEFAULT_COUNT = 200;
    private static final int MAX_COUNT = 500;

    private final CompanyRepository companyRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${finnhub.api.key}")
    private String apiKey;

    public IndicatorResponseDto getIndicators(String ticker,
                                              String resolution,
                                              int requestedCount,
                                              List<Integer> smaPeriods,
                                              List<Integer> emaPeriods,
                                              List<Integer> rsiPeriods) {

        ensureTickerExists(ticker);

        List<Integer> safeSma = sanitisePeriods(smaPeriods, 20);
        List<Integer> safeEma = sanitisePeriods(emaPeriods, 20);
        List<Integer> safeRsi = sanitisePeriods(rsiPeriods, 14);

        int maxPeriod = Stream.of(safeSma.stream(), safeEma.stream(), safeRsi.stream())
                .flatMapToInt(s -> s.mapToInt(Integer::intValue))
                .max()
                .orElse(1);

        int count = Math.max(maxPeriod + 10, requestedCount > 0 ? requestedCount : DEFAULT_COUNT);
        count = Math.min(count, MAX_COUNT);

        FinnhubCandleResponse candles = fetchCandles(ticker, resolution, count);
        if (!candles.isOk()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No candle data available for ticker " + ticker);
        }

        List<Double> closes = candles.getClose();
        List<Long> timestamps = candles.getTimestamp();
        if (closes == null || timestamps == null || closes.size() != timestamps.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Finnhub candle response malformed");
        }

        Map<String, Double[]> overlayValues = new LinkedHashMap<>();

        safeSma.forEach(period -> overlayValues.put("sma-" + period,
                IndicatorCalculator.simpleMovingAverage(closes, period)));
        safeEma.forEach(period -> overlayValues.put("ema-" + period,
                IndicatorCalculator.exponentialMovingAverage(closes, period)));
        safeRsi.forEach(period -> overlayValues.put("rsi-" + period,
                IndicatorCalculator.relativeStrengthIndex(closes, period)));

        List<IndicatorDefinitionDto> overlays = overlayValues.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    String[] parts = key.split("-");
                    String type = parts[0].toUpperCase();
                    int period = Integer.parseInt(parts[1]);
                    String label = type + " (" + period + ")";
                    return new IndicatorDefinitionDto(key, label, type, period);
                })
                .collect(Collectors.toList());

        List<IndicatorPointDto> points = new ArrayList<>(closes.size());
        for (int i = 0; i < closes.size(); i++) {
            Map<String, Double> valueMap = new LinkedHashMap<>();
            for (Map.Entry<String, Double[]> entry : overlayValues.entrySet()) {
                Double value = entry.getValue()[i];
                if (value != null) {
                    valueMap.put(entry.getKey(), value);
                }
            }
            IndicatorPointDto point = new IndicatorPointDto(
                    timestamps.get(i) * 1000L,
                    closes.get(i),
                    valueMap
            );
            points.add(point);
        }

        return new IndicatorResponseDto(ticker, resolution, overlays, points);
    }

    private void ensureTickerExists(String ticker) {
        if (!companyRepository.existsById(ticker)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown ticker: " + ticker);
        }
    }

    private List<Integer> sanitisePeriods(List<Integer> periods, int defaultPeriod) {
        if (periods == null || periods.isEmpty()) {
            return List.of(defaultPeriod);
        }
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer value : periods) {
            if (value == null) {
                continue;
            }
            int p = Math.abs(value);
            if (p >= 2 && p <= 365) {
                unique.add(p);
            }
        }
        if (unique.isEmpty()) {
            unique.add(defaultPeriod);
        }
        return new ArrayList<>(unique);
    }

    @Cacheable(value = "indicatorCandles", key = "#ticker + ':' + #resolution + ':' + #count")
    protected FinnhubCandleResponse fetchCandles(String ticker, String resolution, int count) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://finnhub.io/api/v1/stock/candle")
                .queryParam("symbol", ticker)
                .queryParam("resolution", resolution)
                .queryParam("count", count)
                .queryParam("token", apiKey)
                .build(true)
                .toUri();
        try {
            FinnhubCandleResponse response = restTemplate.getForObject(uri, FinnhubCandleResponse.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Finnhub candle response empty");
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("Finnhub candle request failed for {}: {}", ticker, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch candles for ticker " + ticker);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class FinnhubCandleResponse {
        @JsonProperty("c")
        private List<Double> close = Collections.emptyList();
        @JsonProperty("t")
        private List<Long> timestamp = Collections.emptyList();
        @JsonProperty("s")
        private String status;

        public List<Double> getClose() {
            return close;
        }

        public List<Long> getTimestamp() {
            return timestamp;
        }

        public boolean isOk() {
            return "ok".equalsIgnoreCase(status) && !close.isEmpty() && close.size() == timestamp.size();
        }
    }
}
