package com.example.finsight_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IpoScheduledService {

    private final IpoService ipoService;

    public IpoScheduledService(IpoService ipoService) {
        this.ipoService = ipoService;
    }

    /**
     * Scheduled task to fetch IPOs daily at 6:00 AM
     * This will fetch IPOs for today and tomorrow
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void fetchDailyIpos() {
        try {
            log.info("Starting scheduled IPO fetch task");
            ipoService.fetchAndStoreIpos();
            log.info("Scheduled IPO fetch task completed successfully");
        } catch (Exception e) {
            log.error("Error in scheduled IPO fetch task", e);
        }
    }

    /**
     * Manual trigger method for testing purposes
     * Can be called via REST endpoint if needed
     */
    public void triggerIpoFetch() {
        try {
            log.info("Manual IPO fetch triggered");
            ipoService.fetchAndStoreIpos();
            log.info("Manual IPO fetch completed successfully");
        } catch (Exception e) {
            log.error("Error in manual IPO fetch", e);
            throw e;
        }
    }
}
