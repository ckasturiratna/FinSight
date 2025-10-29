package com.example.finsight_backend.service;

import com.example.finsight_backend.entity.Alert;
import com.example.finsight_backend.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class AlertEvaluationService {

    private final AlertRepository alertRepository;
    private final PriceService priceService;
    private final NotificationService notificationService;
    private static final int COOLDOWN_MINUTES = 5;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void evaluateAlerts() {
        log.info("--- Running scheduled alert evaluation job ---");
        List<Alert> alerts = alertRepository.findActiveAlertsReadyForEvaluation();

        for (Alert alert : alerts) {
            priceService.getLastPrice(alert.getCompany().getTicker()).ifPresent(priceQuote -> {
                double currentPrice = priceQuote.getPrice();
                boolean triggered = false;

                if ("GT".equalsIgnoreCase(alert.getConditionType()) && currentPrice > alert.getThreshold()) {
                    triggered = true;
                } else if ("LT".equalsIgnoreCase(alert.getConditionType()) && currentPrice < alert.getThreshold()) {
                    triggered = true;
                }

                if (triggered) {
                    log.info("!!! TRIGGERED: Alert {} for {} at price {} !!!", alert.getId(), alert.getCompany().getTicker(), currentPrice);
                    String message = String.format("Price Alert: %s is now $%.2f, triggering your alert for %s $%.2f.",
                            alert.getCompany().getTicker(), currentPrice, alert.getConditionType(), alert.getThreshold());

                    notificationService.createAndSendNotification(alert.getUser(), message, alert);

                    // Apply cooldown to prevent spam
                    alert.setCooldownExpiresAt(LocalDateTime.now().plusMinutes(COOLDOWN_MINUTES));
                    alertRepository.save(alert);
                }
            });
        }
        log.info("--- Finished alert evaluation job ---");
    }
}
