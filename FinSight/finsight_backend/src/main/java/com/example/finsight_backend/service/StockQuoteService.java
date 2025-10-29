package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.StockPriceDto;
import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.entity.StockQuote;
import com.example.finsight_backend.repository.CompanyRepository;
import com.example.finsight_backend.repository.StockQuoteRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class StockQuoteService {

    private static final Logger log = LoggerFactory.getLogger(StockQuoteService.class);

    private final StockQuoteRepository stockQuoteRepository;
    private final CompanyRepository companyRepository;
    private final RestTemplate restTemplate;

    @Value("${finnhub.api.key}")
    private String apiKey;

    public StockQuoteService(StockQuoteRepository stockQuoteRepository, CompanyRepository companyRepository) {
        this.stockQuoteRepository = stockQuoteRepository;
        this.companyRepository = companyRepository;
        this.restTemplate = new RestTemplate();
    }

    public StockPriceDto getPrice(String ticker) {
        log.info("Fetching price for ticker: {}", ticker);

        // Verify ticker exists in companies
        Optional<Company> company = companyRepository.findById(ticker);
        if (company.isEmpty()) {
            log.warn("Ticker {} not found in companies", ticker);
            throw new IllegalArgumentException("Invalid ticker");
        }

        // Check DB for recent quote
        Optional<StockQuote> latestQuote = stockQuoteRepository.findTopByCompanyTickerOrderByTimestampDesc(ticker);
        if (latestQuote.isPresent() && latestQuote.get().getTimestamp().isAfter(LocalDateTime.now().minusSeconds(5))) {
            log.debug("Returning cached quote from DB for {}", ticker);
            return toDto(latestQuote.get(), ticker);
        }

        // Fetch from Finnhub
        StockQuote fetched = fetchFromFinnhub(ticker);
        if (fetched != null) {
            fetched.setCompany(company.get());
            stockQuoteRepository.save(fetched);
            log.info("Saved new quote for {}", ticker);
            return toDto(fetched, ticker);
        }

        log.warn("No price data available for {}", ticker);
        throw new IllegalArgumentException("No price data available for ticker");
    }

    @Retry(name = "finnhub")
    private StockQuote fetchFromFinnhub(String ticker) {
        String quoteUrl = "https://finnhub.io/api/v1/quote?symbol=" + ticker + "&token=" + apiKey;
        log.debug("Calling Finnhub: {}", quoteUrl);

        Map<String, Object> quote;
        try {
            quote = restTemplate.getForObject(quoteUrl, Map.class);
            log.debug("Finnhub response for {}: {}", ticker, quote); // Log full response
        } catch (Exception e) {
            log.error("Finnhub error for {}: {}", ticker, e.getMessage());
            return null;
        }

        if (quote == null || quote.isEmpty() || quote.get("c") == null || quote.get("error") != null) {
            log.warn("Empty, invalid, or error response for {}: {}", ticker, quote != null ? quote.get("error") : "null response");
            return null;
        }

        // Validate required fields (currentPrice is mandatory)
        if (quote.get("c") == null) {
            log.warn("Missing currentPrice in Finnhub response for {}: {}", ticker, quote);
            return null;
        }

        StockQuote stockQuote = new StockQuote();
        stockQuote.setCurrentPrice(((Number) quote.get("c")).doubleValue());
        // Volume is optional
        if (quote.get("v") != null) {
            stockQuote.setVolume(((Number) quote.get("v")).longValue());
        } else {
            stockQuote.setVolume(0L); // Default to 0 if volume is missing
            log.debug("Volume missing for {}, defaulting to 0", ticker);
        }
        // Other fields are optional
        if (quote.get("dp") != null) {
            stockQuote.setPercentChange(((Number) quote.get("dp")).doubleValue());
        }
        if (quote.get("h") != null) {
            stockQuote.setHigh(((Number) quote.get("h")).doubleValue());
        }
        if (quote.get("l") != null) {
            stockQuote.setLow(((Number) quote.get("l")).doubleValue());
        }
        stockQuote.setTimestamp(LocalDateTime.now());
        return stockQuote;
    }

    private StockPriceDto toDto(StockQuote stockQuote, String ticker) {
        StockPriceDto dto = new StockPriceDto();
        dto.setTicker(ticker);
        dto.setCurrentPrice(stockQuote.getCurrentPrice());
        dto.setVolume(stockQuote.getVolume());
        dto.setPercentChange(stockQuote.getPercentChange());
        dto.setHigh(stockQuote.getHigh());
        dto.setLow(stockQuote.getLow());
        dto.setTimestamp(stockQuote.getTimestamp());
        return dto;
    }
}