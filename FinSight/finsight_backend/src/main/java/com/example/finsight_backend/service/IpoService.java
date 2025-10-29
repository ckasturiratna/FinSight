package com.example.finsight_backend.service;

import com.example.finsight_backend.entity.IpoCalendar;
import com.example.finsight_backend.repository.IpoCalendarRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class IpoService {

    private final IpoCalendarRepository repository;
    private final RestTemplate restTemplate;

    @Value("${finnhub.api.key}")
    private String apiKey;

    private static final String FINNHUB_IPO_URL = "https://finnhub.io/api/v1/calendar/ipo";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public IpoService(IpoCalendarRepository repository) {
        this.repository = repository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetch IPOs from Finnhub API for today and tomorrow
     */
    public void fetchAndStoreIpos() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            
            log.info("Fetching IPOs for dates: {} to {}", today, tomorrow);
            
            String url = String.format("%s?from=%s&to=%s&token=%s", 
                FINNHUB_IPO_URL, 
                today.format(DATE_FORMATTER), 
                tomorrow.format(DATE_FORMATTER), 
                apiKey);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("ipoCalendar")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ipoList = (List<Map<String, Object>>) response.get("ipoCalendar");
                
                int savedCount = 0;
                int skippedCount = 0;
                
                for (Map<String, Object> ipoData : ipoList) {
                    try {
                        IpoCalendar ipo = mapToIpoCalendar(ipoData);
                        
                        // Check if IPO already exists by symbol
                        if (!repository.existsBySymbol(ipo.getSymbol())) {
                            repository.save(ipo);
                            savedCount++;
                            log.debug("Saved new IPO: {} ({})", ipo.getName(), ipo.getSymbol());
                        } else {
                            skippedCount++;
                            log.debug("Skipped existing IPO: {} ({})", ipo.getName(), ipo.getSymbol());
                        }
                    } catch (Exception e) {
                        log.error("Error processing IPO data: {}", ipoData, e);
                    }
                }
                
                log.info("IPO fetch completed. Saved: {}, Skipped: {}", savedCount, skippedCount);
            } else {
                log.warn("No IPO data received from Finnhub API");
            }
            
        } catch (RestClientException e) {
            log.error("Error fetching IPOs from Finnhub API", e);
            throw new RuntimeException("Failed to fetch IPO data from Finnhub API", e);
        } catch (Exception e) {
            log.error("Unexpected error during IPO fetch", e);
            throw new RuntimeException("Unexpected error during IPO fetch", e);
        }
    }

    /**
     * Map Finnhub API response to IpoCalendar entity
     */
    private IpoCalendar mapToIpoCalendar(Map<String, Object> ipoData) {
        IpoCalendar ipo = new IpoCalendar();
        
        // Symbol (required, use as unique identifier)
        String symbol = (String) ipoData.get("symbol");
        if (symbol == null || symbol.trim().isEmpty()) {
            // Generate a unique symbol if not provided
            symbol = "IPO_" + System.currentTimeMillis() + "_" + Math.random();
        }
        ipo.setSymbol(symbol);
        
        // Name (required)
        String name = (String) ipoData.get("name");
        if (name == null || name.trim().isEmpty()) {
            name = "Unknown Company";
        }
        ipo.setName(name);
        
        // Date (required)
        String dateStr = (String) ipoData.get("date");
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            ipo.setDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        } else {
            ipo.setDate(LocalDate.now());
        }
        
        // Exchange
        ipo.setExchange((String) ipoData.get("exchange"));
        
        // Number of shares
        Object numberOfShares = ipoData.get("numberOfShares");
        if (numberOfShares != null) {
            if (numberOfShares instanceof Number) {
                ipo.setNumberOfShares(((Number) numberOfShares).longValue());
            } else if (numberOfShares instanceof String) {
                try {
                    ipo.setNumberOfShares(Long.parseLong((String) numberOfShares));
                } catch (NumberFormatException e) {
                    log.warn("Invalid numberOfShares format: {}", numberOfShares);
                }
            }
        }
        
        // Price
        Object price = ipoData.get("price");
        if (price != null) {
            ipo.setPrice(price.toString());
        }
        
        // Status
        ipo.setStatus((String) ipoData.get("status"));
        
        // Total shares value
        Object totalSharesValue = ipoData.get("totalSharesValue");
        if (totalSharesValue != null) {
            if (totalSharesValue instanceof Number) {
                ipo.setTotalSharesValue(BigDecimal.valueOf(((Number) totalSharesValue).doubleValue()));
            } else if (totalSharesValue instanceof String) {
                try {
                    ipo.setTotalSharesValue(new BigDecimal((String) totalSharesValue));
                } catch (NumberFormatException e) {
                    log.warn("Invalid totalSharesValue format: {}", totalSharesValue);
                }
            }
        }
        
        return ipo;
    }

    /**
     * Get all IPOs from database
     */
    public List<IpoCalendar> getAllIpos() {
        return repository.findAll();
    }

    /**
     * Get IPOs for today and tomorrow
     */
    public List<IpoCalendar> getUpcomingIpos() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        return repository.findUpcomingIpos(today, tomorrow);
    }

    /**
     * Get IPOs by date range
     */
    public List<IpoCalendar> getIposByDateRange(LocalDate startDate, LocalDate endDate) {
        return repository.findByDateBetween(startDate, endDate);
    }

    /**
     * Get IPOs by status
     */
    public List<IpoCalendar> getIposByStatus(String status) {
        return repository.findByStatus(status);
    }

    /**
     * Get IPOs by exchange
     */
    public List<IpoCalendar> getIposByExchange(String exchange) {
        return repository.findByExchange(exchange);
    }
}
