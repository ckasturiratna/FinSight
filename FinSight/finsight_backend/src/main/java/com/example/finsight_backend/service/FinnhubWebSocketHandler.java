package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.StockPriceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FinnhubWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FinnhubWebSocketHandler.class);

    private final WebSocketSession clientSession;
    private final StockQuoteService stockQuoteService;
    private final String ticker;
    private final ObjectMapper objectMapper;
    private ScheduledExecutorService restScheduler;

    public FinnhubWebSocketHandler(WebSocketSession clientSession, StockQuoteService stockQuoteService, String ticker, ObjectMapper objectMapper) {
        this.clientSession = clientSession;
        this.stockQuoteService = stockQuoteService;
        this.ticker = ticker;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Finnhub WebSocket connected for ticker: {}", ticker);

        // Schedule REST calls for high/low/percentChange every 5s
        restScheduler = Executors.newScheduledThreadPool(1);
        restScheduler.scheduleAtFixedRate(() -> {
            try {
                if (!clientSession.isOpen()) {
                    log.debug("Client session closed; stopping REST scheduler for {}", ticker);
                    restScheduler.shutdown();
                    return;
                }
                StockPriceDto restData = stockQuoteService.getPrice(ticker);
                clientSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(restData)));
            } catch (Exception e) {
                log.error("Error sending REST data for {}: {}", ticker, e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> finnhubData = objectMapper.readValue(message.getPayload(), Map.class);
            if ("trade".equals(finnhubData.get("type"))) {
                for (Map<String, Object> trade : (Iterable<Map<String, Object>>) finnhubData.get("data")) {
                    StockPriceDto dto = new StockPriceDto();
                    dto.setTicker((String) trade.get("s"));
                    dto.setCurrentPrice(((Number) trade.get("p")).doubleValue());
                    dto.setVolume(((Number) trade.get("v")).longValue());
                    dto.setTimestamp(LocalDateTime.now());
                    // High/low/percentChange from REST scheduler
                    String json = objectMapper.writeValueAsString(dto);
                    clientSession.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Finnhub message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Finnhub WebSocket closed: {}", status);
        if (restScheduler != null) {
            restScheduler.shutdown();
        }
        clientSession.close(status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Finnhub WebSocket error: {}", exception.getMessage());
        clientSession.close(CloseStatus.SERVER_ERROR);
    }
}
