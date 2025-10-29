package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.StockPriceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PriceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(PriceWebSocketHandler.class);

    @Value("${finnhub.api.key}")
    private String apiKey;

    @Value("${simulator.enabled:false}")
    private boolean simulatorEnabled;

    private final ObjectMapper objectMapper;
    private final StockQuoteService stockQuoteService;

    private ScheduledExecutorService simulatorScheduler;
    private final AtomicReference<WebSocketSession> finnhubSessionRef = new AtomicReference<>();
    private volatile CompletableFuture<WebSocketSession> finnhubFuture;

    @Autowired
    public PriceWebSocketHandler(StockQuoteService stockQuoteService, ObjectMapper objectMapper) {
        this.stockQuoteService = stockQuoteService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticker = session.getUri().getPath().split("/")[3];
        log.info("WebSocket connected for ticker: {}", ticker);

        if (simulatorEnabled) {
            simulatorScheduler = Executors.newScheduledThreadPool(1);
            simulatorScheduler.scheduleAtFixedRate(() -> {
                try {
                    session.sendMessage(new TextMessage(generateMockTick(ticker)));
                } catch (IOException e) {
                    log.error("Simulator error for {}: {}", ticker, e.getMessage());
                }
            }, 0, 1, TimeUnit.SECONDS);
        } else {
            StandardWebSocketClient client = new StandardWebSocketClient();
            finnhubFuture = client.execute(new FinnhubWebSocketHandler(session, stockQuoteService, ticker, objectMapper), "wss://ws.finnhub.io?token=" + apiKey);
            finnhubFuture.thenAccept(finnhubSession -> {
                finnhubSessionRef.set(finnhubSession);
                try {
                    finnhubSession.sendMessage(new TextMessage("{\"type\":\"subscribe\",\"symbol\":\"" + ticker + "\"}"));
                } catch (IOException e) {
                    log.error("Failed to subscribe to {}: {}", ticker, e.getMessage());
                }
            }).exceptionally(ex -> {
                log.error("Finnhub connection failed for {}: {}", ticker, ex.getMessage());
                return null;
            });
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket error for {}: {}", session.getUri(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket closed for {}", session.getUri());
        if (simulatorScheduler != null) {
            simulatorScheduler.shutdown();
        }
        // Close upstream Finnhub connection and cancel pending connect
        try {
            WebSocketSession upstream = finnhubSessionRef.getAndSet(null);
            if (upstream != null && upstream.isOpen()) {
                String ticker = session.getUri().getPath().split("/")[3];
                String unsub = "{\"type\":\"unsubscribe\",\"symbol\":\"" + ticker + "\"}";
                upstream.sendMessage(new TextMessage(unsub));
                upstream.close(CloseStatus.NORMAL);
            }
        } catch (Exception e) {
            log.warn("Error closing upstream Finnhub session: {}", e.getMessage());
        }
        try {
            CompletableFuture<WebSocketSession> future = finnhubFuture;
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        } catch (Exception ignore) {
        }
    }

    private String generateMockTick(String ticker) {
        StockPriceDto dto = new StockPriceDto();
        dto.setTicker(ticker);
        dto.setCurrentPrice(150 + Math.random() * 5 - 2.5);
        dto.setVolume(1000L);
        dto.setPercentChange(Math.random() * 2 - 1);
        dto.setHigh(dto.getCurrentPrice() + 1);
        dto.setLow(dto.getCurrentPrice() - 1);
        dto.setTimestamp(LocalDateTime.now());
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            log.error("Failed to serialize mock tick: {}", e.getMessage());
            return "{}";
        }
    }
}
