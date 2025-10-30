package com.example.finsight_backend.controller;

import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.UserRepository;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        log.info("Fetching all users from database");
        List<User> users = userRepository.findAll();
        log.info("Found {} users in database", users.size());
        return users;
    }

    @GetMapping("/test-db")
    public String testDatabase() {
        try {
            long count = userRepository.count();
            return "Database connection successful. Total users: " + count;
        } catch (Exception e) {
            log.error("Database test failed", e);
            return "Database test failed: " + e.getMessage();
        }
    }

    @GetMapping("/oauth-flow")
    public String testOAuthFlow(HttpServletRequest request) {
        String oauthUrl = UriComponentsBuilder.fromUriString(buildBackendBaseUrl(request))
                .path("/oauth2/authorization/google")
                .build()
                .toUriString();
        return "OAuth flow test - try: " + oauthUrl;
    }

    @GetMapping("/session-info")
    public String getSessionInfo(HttpServletRequest request) {
        try {
            String jwtToken = (String) request.getSession().getAttribute("jwt_token");
            return "Session ID: " + request.getSession().getId() + 
                   ", JWT Token: " + (jwtToken != null ? jwtToken.substring(0, Math.min(20, jwtToken.length())) + "..." : "null");
        } catch (Exception e) {
            return "Session error: " + e.getMessage();
        }
    }

    @GetMapping("/oauth-test")
    public String testOAuth(HttpServletRequest request) {
        String oauthUrl = UriComponentsBuilder.fromUriString(buildBackendBaseUrl(request))
                .path("/oauth2/authorization/google")
                .build()
                .toUriString();
        return "OAuth test endpoint - try: " + oauthUrl;
    }

    private String buildBackendBaseUrl(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(contextPath != null ? contextPath : "")
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
