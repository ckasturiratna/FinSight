package com.example.finsight_backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight candle fetcher for daily closes from Finnhub.
 * Used to backfill portfolio history without waiting for scheduled snapshots.
 */
@Service
@Slf4j
public class CandleService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${finnhub.api.key}")
    private String apiKey;

    /**
     * Fetches up to {@code days} daily close prices and returns a date->close map (UTC dates, ascending order preserved).
     */
    @Cacheable(value = "dailyCandles", key = "#ticker + ':' + #days")
    public Map<LocalDate, Double> getDailyCloses(String ticker, int days) {
        if (days <= 0) days = 90;
        URI uri = UriComponentsBuilder.fromHttpUrl("https://finnhub.io/api/v1/stock/candle")
                .queryParam("symbol", ticker)
                .queryParam("resolution", "D")
                .queryParam("count", days)
                .queryParam("token", apiKey)
                .build(true)
                .toUri();
        try {
            FinnhubCandleResponse response = restTemplate.getForObject(uri, FinnhubCandleResponse.class);
            if (response == null || !response.isOk()) {
                log.warn("Empty/malformed candle response for {}", ticker);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No candle data for ticker " + ticker);
            }
            List<Double> close = response.getClose();
            List<Long> ts = response.getTimestamp();
            Map<LocalDate, Double> out = new LinkedHashMap<>();
            for (int i = 0; i < close.size(); i++) {
                long epochSec = ts.get(i);
                LocalDate date = Instant.ofEpochSecond(epochSec).atZone(ZoneOffset.UTC).toLocalDate();
                out.put(date, close.get(i)); // daily resolution => one bar per date
            }
            return out;
        } catch (RestClientException ex) {
            log.warn("Finnhub candle request failed for {}: {}", ticker, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch candles for ticker " + ticker);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class FinnhubCandleResponse {
        @JsonProperty("c")
        private List<Double> close;
        @JsonProperty("t")
        private List<Long> timestamp;
        @JsonProperty("s")
        private String status;

        public List<Double> getClose() { return close; }
        public List<Long> getTimestamp() { return timestamp; }
        public boolean isOk() {
            return "ok".equalsIgnoreCase(status) && close != null && timestamp != null && close.size() == timestamp.size() && !close.isEmpty();
        }
    }
}

