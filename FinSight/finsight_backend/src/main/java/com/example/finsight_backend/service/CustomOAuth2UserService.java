package com.example.finsight_backend.service;

import com.example.finsight_backend.entity.Role;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.UserRepository;
import com.example.finsight_backend.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

// @Service - Temporarily disabled to use SimpleOAuth2UserService
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user");
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        log.info("Processing OAuth2 user from provider: {}", provider);
        log.info("OAuth2 user attributes: {}", attributes);
        
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub");
        
        log.info("Extracted user info - Email: {}, Name: {}, ProviderId: {}", email, name, providerId);
        
        if (email == null || email.isEmpty()) {
            log.error("Email not found in OAuth2 attributes: {}", attributes);
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                // Create new user if doesn't exist
                log.info("Creating new user for email: {}", email);
                String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"OAuth", "User"};
                String firstName = nameParts.length > 0 && !nameParts[0].isEmpty() ? nameParts[0] : "OAuth";
                String lastName = nameParts.length > 1 && !nameParts[1].isEmpty() ? nameParts[1] : "User";
                
                User newUser = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .provider(provider)
                    .providerId(providerId)
                    .role(Role.USER)
                    .build();
                
                log.info("Saving new user: {}", newUser);
                try {
                    User savedUser = userRepository.save(newUser);
                    log.info("User saved successfully with ID: {}", savedUser.getId());
                    return savedUser;
                } catch (Exception e) {
                    log.error("Failed to save user: {}", e.getMessage(), e);
                    throw new OAuth2AuthenticationException("Failed to create user: " + e.getMessage());
                }
            });

        // Update provider info if user exists but doesn't have OAuth provider set
        if (user.getProvider() == null || !user.getProvider().equals(provider)) {
            log.info("Updating provider info for existing user: {}", email);
            user.setProvider(provider);
            user.setProviderId(providerId);
            userRepository.save(user);
        }

        // Generate JWT token
        log.info("Generating JWT token for user: {}", email);
        String jwtToken = jwtTokenProvider.generateToken(
            user.getEmail(),
            user.getRole().name(),
            user.getFirstName(),
            user.getLastName()
        );
        log.info("JWT token generated successfully");

        // Store token in session for redirect
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            request.getSession().setAttribute("jwt_token", jwtToken);
            log.info("JWT token stored in session successfully");
        } catch (Exception e) {
            log.error("Failed to store JWT token in session", e);
            throw new OAuth2AuthenticationException("Failed to store authentication token");
        }

        log.info("OAuth2 user processed successfully: {}", email);
        return oauth2User;
    }
}
