package com.example.finsight_backend.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${finnhub.api.key}")
    private String apiKey;

    @Data
    @AllArgsConstructor
    public static class PriceQuote {
        private Double price;
        private Instant asOf;
    }

    @Cacheable(value = "quotes", key = "#ticker", unless = "#result == null")
    public Optional<PriceQuote> getLastPrice(String ticker) {
        try {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + ticker + "&token=" + apiKey;
            Map body = restTemplate.getForObject(url, Map.class);
            if (body == null) return Optional.empty();
            Object c = body.get("c"); // current price
            Object t = body.get("t"); // timestamp seconds
            if (c instanceof Number) {
                Double price = ((Number) c).doubleValue();
                Instant asOf = (t instanceof Number) ? Instant.ofEpochSecond(((Number) t).longValue()) : Instant.now();
                return Optional.of(new PriceQuote(price, asOf));
            }
        } catch (Exception ex) {
            log.warn("Quote fetch failed for {}: {}", ticker, ex.getMessage());
        }
        return Optional.empty();
    }
}

