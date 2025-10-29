package com.example.finsight_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Long id;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private Long alertId;
}
