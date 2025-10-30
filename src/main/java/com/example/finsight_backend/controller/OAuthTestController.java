package com.example.finsight_backend.controller;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/oauth-test")
public class OAuthTestController {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @GetMapping("/config")
    public Map<String, String> getOAuthConfig(HttpServletRequest request) {
        Map<String, String> config = new HashMap<>();
        config.put("clientId", clientId != null ? clientId.substring(0, Math.min(20, clientId.length())) + "..." : "null");
        config.put("clientSecret", clientSecret != null ? clientSecret.substring(0, Math.min(10, clientSecret.length())) + "..." : "null");
        String redirectUri = UriComponentsBuilder.fromUriString(buildBackendBaseUrl(request))
                .path("/login/oauth2/code/google")
                .build()
                .toUriString();
        config.put("redirectUri", redirectUri);
        return config;
    }

    @GetMapping("/test-url")
    public Map<String, String> getTestUrl(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        String oauthUrl = UriComponentsBuilder.fromUriString(buildBackendBaseUrl(request))
                .path("/oauth2/authorization/google")
                .build()
                .toUriString();
        result.put("oauthUrl", oauthUrl);
        result.put("message", "Click this URL to test OAuth flow");
        return result;
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
