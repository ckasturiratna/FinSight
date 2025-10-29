package com.example.finsight_backend.controller;

import com.example.finsight_backend.entity.IpoCalendar;
import com.example.finsight_backend.service.IpoService;
import com.example.finsight_backend.service.IpoScheduledService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ipo")
@Slf4j
public class IpoController {

    private final IpoService ipoService;
    private final IpoScheduledService ipoScheduledService;

    public IpoController(IpoService ipoService, IpoScheduledService ipoScheduledService) {
        this.ipoService = ipoService;
        this.ipoScheduledService = ipoScheduledService;
    }

    /**
     * Manually trigger IPO fetch from Finnhub API
     */
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchIpos() {
        try {
            ipoScheduledService.triggerIpoFetch();
            return ResponseEntity.ok("IPO fetch completed successfully");
        } catch (Exception e) {
            log.error("Error fetching IPOs", e);
            return ResponseEntity.internalServerError()
                .body("Error fetching IPOs: " + e.getMessage());
        }
    }

    /**
     * Get all IPOs from database
     */
    @GetMapping("/all")
    public ResponseEntity<List<IpoCalendar>> getAllIpos() {
        try {
            List<IpoCalendar> ipos = ipoService.getAllIpos();
            return ResponseEntity.ok(ipos);
        } catch (Exception e) {
            log.error("Error retrieving IPOs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get upcoming IPOs (today and tomorrow)
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<IpoCalendar>> getUpcomingIpos() {
        try {
            List<IpoCalendar> ipos = ipoService.getUpcomingIpos();
            return ResponseEntity.ok(ipos);
        } catch (Exception e) {
            log.error("Error retrieving upcoming IPOs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get IPOs by date range
     */
    @GetMapping("/range")
    public ResponseEntity<List<IpoCalendar>> getIposByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<IpoCalendar> ipos = ipoService.getIposByDateRange(startDate, endDate);
            return ResponseEntity.ok(ipos);
        } catch (Exception e) {
            log.error("Error retrieving IPOs by date range", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get IPOs by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<IpoCalendar>> getIposByStatus(@PathVariable String status) {
        try {
            List<IpoCalendar> ipos = ipoService.getIposByStatus(status);
            return ResponseEntity.ok(ipos);
        } catch (Exception e) {
            log.error("Error retrieving IPOs by status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get IPOs by exchange
     */
    @GetMapping("/exchange/{exchange}")
    public ResponseEntity<List<IpoCalendar>> getIposByExchange(@PathVariable String exchange) {
        try {
            List<IpoCalendar> ipos = ipoService.getIposByExchange(exchange);
            return ResponseEntity.ok(ipos);
        } catch (Exception e) {
            log.error("Error retrieving IPOs by exchange", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
