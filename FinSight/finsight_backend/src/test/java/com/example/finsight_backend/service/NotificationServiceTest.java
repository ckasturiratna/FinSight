package com.example.finsight_backend.service;

import com.example.finsight_backend.entity.Notification;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.NotificationRepository;
import com.example.finsight_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationWebSocketHandler notificationWebSocketHandler;
    @Mock
    private HtmlEmailService htmlEmailService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");

        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setMessage("Test message");

        // Mock the security context to simulate a logged-in user
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(authentication.getName()).thenReturn(testUser.getEmail());
        lenient().when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
    }

    @Test
    void whenCreateAndSendNotification_thenAllServicesShouldBeCalled() throws Exception {
        // Arrange
        String message = "Test message";
        String expectedJsonPayload = "{\"id\":1,\"message\":\"Test message\",\"read\":false,\"createdAt\":null}";

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJsonPayload);

        // Act
        notificationService.createAndSendNotification(testUser, message);

        // Assert
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(notificationWebSocketHandler, times(1)).sendMessageToUser(testUser.getEmail(), expectedJsonPayload);
        verify(htmlEmailService, times(1)).sendPriceAlertEmail(eq(testUser.getEmail()), eq(testUser.getFirstName()), eq(message), anyString(), anyString());
    }

    @Test
    void whenGetNotifications_withNoDates_thenCallsDefaultMethod() {
        // Act
        notificationService.getNotificationsForCurrentUser(null, null);

        // Assert
        verify(notificationRepository, times(1)).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void whenGetNotifications_withDateRange_thenCallsBetweenMethod() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        // Act
        notificationService.getNotificationsForCurrentUser(start, end);

        // Assert
        verify(notificationRepository, times(1)).findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(testUser, start, end);
    }

    @Test
    void whenGetNotifications_withStartDateOnly_thenCallsAfterMethod() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(1);

        // Act
        notificationService.getNotificationsForCurrentUser(start, null);

        // Assert
        verify(notificationRepository, times(1)).findByUserAndCreatedAtAfterOrderByCreatedAtDesc(testUser, start);
    }

    @Test
    void whenGetNotifications_withEndDateOnly_thenCallsBeforeMethod() {
        // Arrange
        LocalDateTime end = LocalDateTime.now();

        // Act
        notificationService.getNotificationsForCurrentUser(null, end);

        // Assert
        verify(notificationRepository, times(1)).findByUserAndCreatedAtBeforeOrderByCreatedAtDesc(testUser, end);
    }
}
