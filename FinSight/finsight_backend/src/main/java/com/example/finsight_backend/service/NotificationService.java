package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.NotificationDto;
import com.example.finsight_backend.entity.Alert;
import com.example.finsight_backend.entity.Notification;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.NotificationRepository;
import com.example.finsight_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final HtmlEmailService htmlEmailService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createAndSendNotification(User user, String message, Alert alert) {
        // 1. Save notification to the database
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setAlert(alert); // Associate the alert
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Saved notification for user {}: {}", user.getEmail(), message);

        // 2. Push notification via WebSocket
        try {
            NotificationDto notificationDto = toDto(savedNotification);
            String notificationPayload = objectMapper.writeValueAsString(notificationDto);
            notificationWebSocketHandler.sendMessageToUser(user.getEmail(), notificationPayload);
        } catch (Exception e) {
            log.error("Error sending WebSocket notification to user {}: {}", user.getEmail(), e.getMessage());
        }

        // 3. Send HTML email
        try {
            // Extract company name and price from message if possible
            String companyName = extractCompanyName(message);
            String currentPrice = extractPrice(message);

            htmlEmailService.sendPriceAlertEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    message,
                    companyName,
                    currentPrice);
            log.info("Sent HTML email notification to user {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error sending HTML email notification to user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public List<NotificationDto> getNotificationsForCurrentUser(LocalDateTime startDate, LocalDateTime endDate) {
        User user = getCurrentUser();
        List<Notification> notifications;

        if (startDate != null && endDate != null) {
            notifications = notificationRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, startDate,
                    endDate);
        } else if (startDate != null) {
            notifications = notificationRepository.findByUserAndCreatedAtAfterOrderByCreatedAtDesc(user, startDate);
        } else if (endDate != null) {
            notifications = notificationRepository.findByUserAndCreatedAtBeforeOrderByCreatedAtDesc(user, endDate);
        } else {
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        }

        return notifications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAllNotificationsAsRead() {
        User user = getCurrentUser();
        notificationRepository.markAllAsReadForUser(user.getId());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setMessage(notification.getMessage());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        if (notification.getAlert() != null) {
            dto.setAlertId(notification.getAlert().getId());
        }
        return dto;
    }

    private String extractCompanyName(String message) {
        // Try to extract company name from common patterns
        if (message.contains("for")) {
            String[] parts = message.split("for");
            if (parts.length > 1) {
                return parts[1].trim().split(" ")[0];
            }
        }
        return "Stock";
    }

    private String extractPrice(String message) {
        // Try to extract price from common patterns like $123.45
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\d+\\.\\d{2}");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return "$0.00";
    }
}
