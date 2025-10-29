package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.NotificationDto;
import com.example.finsight_backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get user notifications or alert history",
            description = "Retrieves a list of notifications for the authenticated user. Can be filtered by a date range to get alert history.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications")
            }
    )
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotificationsForCurrentUser(
            @Parameter(description = "The start date for the filter range (ISO-8601 format, e.g., '2023-10-27T00:00:00').")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "The end date for the filter range (ISO-8601 format, e.g., '2023-10-28T23:59:59').")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<NotificationDto> notifications = notificationService.getNotificationsForCurrentUser(startDate, endDate);
        return ResponseEntity.ok(notifications);
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all unread notifications for the currently authenticated user as read.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
            }
    )
    @PostMapping("/mark-all-as-read")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        notificationService.markAllNotificationsAsRead();
        return ResponseEntity.ok().build();
    }
}
