package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.PredictionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class StockPredictionService {

    @Value("${predictor.api.url}")
    private String predictorApi;

    private final RestTemplate restTemplate = new RestTemplate();

    public PredictionResponse predict(String ticker) {
        String url = predictorApi + "/predict/" + ticker;
        log.info("Requesting AI prediction from {}", url);
        return restTemplate.getForObject(url, PredictionResponse.class);
    }
}
