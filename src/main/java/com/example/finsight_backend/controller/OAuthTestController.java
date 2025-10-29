package com.example.finsight_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth-test")
public class OAuthTestController {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @GetMapping("/config")
    public Map<String, String> getOAuthConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("clientId", clientId != null ? clientId.substring(0, Math.min(20, clientId.length())) + "..." : "null");
        config.put("clientSecret", clientSecret != null ? clientSecret.substring(0, Math.min(10, clientSecret.length())) + "..." : "null");
        config.put("redirectUri", "http://localhost:8080/login/oauth2/code/google");
        return config;
    }

    @GetMapping("/test-url")
    public Map<String, String> getTestUrl() {
        Map<String, String> result = new HashMap<>();
        result.put("oauthUrl", "http://localhost:8080/oauth2/authorization/google");
        result.put("message", "Click this URL to test OAuth flow");
        return result;
    }
}

