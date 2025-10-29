package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.ForecastDto;
import com.example.finsight_backend.entity.PredictionLog;
import com.example.finsight_backend.repository.PortfolioHoldingRepository;
import com.example.finsight_backend.repository.PredictionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionLoggingService {

    private final ForecastService forecastService;
    private final PredictionLogRepository predictionLogRepository;
    private final PortfolioHoldingRepository portfolioHoldingRepository;

    /**
     * Scheduled task to generate and save daily forecasts for all unique tickers in user portfolios.
     * Runs every minute for demonstration purposes.
     */
    @Scheduled(cron = "0 * * * * ?") // Run every minute
    public void logDailyPredictions() {
        log.info("--- Running daily prediction logging job ---");

        List<String> tickersToTrack = portfolioHoldingRepository.findDistinctTickers();
        log.info("Found {} unique tickers to track across all portfolios.", tickersToTrack.size());

        for (String ticker : tickersToTrack) {
            try {
                // Get the 1-day ahead forecast
                ForecastDto forecast = forecastService.predictWithCI(ticker);
                if (forecast != null && !forecast.getForecastPoints().isEmpty()) {
                    ForecastDto.ForecastPoint tomorrowForecast = forecast.getForecastPoints().get(0);

                    PredictionLog logEntry = new PredictionLog();
                    logEntry.setTicker(ticker);
                    logEntry.setDate(tomorrowForecast.getDate());
                    logEntry.setPredictedPrice(tomorrowForecast.getMean());
                    // actualPrice will be null until updated later

                    predictionLogRepository.save(logEntry);
                    log.info("Logged prediction for {}: ${} on {}", ticker, tomorrowForecast.getMean(), tomorrowForecast.getDate());
                }
            } catch (Exception e) {
                log.error("Failed to log prediction for ticker {}: {}", ticker, e.getMessage());
            }
        }

        log.info("--- Finished daily prediction logging job ---");
    }

    // In a complete implementation, you would also have a scheduled job
    // to run after market close to update the 'actualPrice' for the day.
}
