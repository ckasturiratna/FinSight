package com.example.finsight_backend.config;

import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.entity.Role;
import com.example.finsight_backend.repository.UserRepository;
import com.example.finsight_backend.security.JwtAuthenticationFilter;
import com.example.finsight_backend.security.JwtTokenProvider;
import com.example.finsight_backend.service.SimpleOAuth2UserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final SimpleOAuth2UserService simpleOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**", "/ws/**", "/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register", "/api/users/login", "/api/users/verify-otp", "/api/users/resend-otp", "/api/users/forgot-password", "/api/users/reset-password", "/api/users/debug-verification/**", "/api/users/check-verification/**", "/api/users/health", "/api/test/public", "/api/test/send-test-email", "/api/test/send-test-otp").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(simpleOAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            final String frontendBase = normalizedFrontendUrl();
                            try {
                                System.out.println("=== OAuth2 Success Handler Called ===");
                                System.out.println("Authentication: " + authentication.getName());
                                System.out.println("Authentication details: " + authentication.getDetails());
                                
                                // Generate JWT token directly in the success handler
                                String email = null;
                                String firstName = "OAuth";
                                String lastName = "User";
                                String providerId = authentication.getName();
                                
                                // Try to get user details from authentication
                                if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                                    org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                                        (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                                    
                                    // Get email from OAuth2 attributes
                                    email = oauth2User.getAttribute("email");
                                    if (email == null || email.isEmpty()) {
                                        System.err.println("ERROR: No email found in OAuth2 attributes");
                                        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBase)
                                                .path("/login")
                                                .queryParam("error", "no_email")
                                                .build()
                                                .toUriString();
                                        response.sendRedirect(redirectUrl);
                                        return;
                                    }
                                    
                                    String name = oauth2User.getAttribute("name");
                                    if (name != null && !name.isEmpty()) {
                                        String[] nameParts = name.split(" ", 2);
                                        firstName = nameParts.length > 0 ? nameParts[0] : "OAuth";
                                        lastName = nameParts.length > 1 ? nameParts[1] : "User";
                                    }
                                } else {
                                    System.err.println("ERROR: Authentication principal is not OAuth2User");
                                    String redirectUrl = UriComponentsBuilder.fromUriString(frontendBase)
                                            .path("/login")
                                            .queryParam("error", "invalid_principal")
                                            .build()
                                            .toUriString();
                                    response.sendRedirect(redirectUrl);
                                    return;
                                }
                                
                                System.out.println("Generating JWT for: " + email + " (" + firstName + " " + lastName + ")");
                                
                                // Save or update user in database
                                User user = userRepository.findByEmail(email).orElse(null);
                                if (user == null) {
                                    // Create new user
                                    LocalDateTime now = LocalDateTime.now();
                                    user = User.builder()
                                            .email(email)
                                            .firstName(firstName)
                                            .lastName(lastName)
                                            .provider("google")
                                            .providerId(providerId) // Use Google user ID
                                            .role(Role.USER)
                                            .createdAt(now)
                                            .updatedAt(now)
                                            .build();
                                    userRepository.save(user);
                                    System.out.println("Created new OAuth user: " + email);
                                } else {
                                    // Update existing user with OAuth info
                                    user.setProvider("google");
                                    user.setProviderId(providerId);
                                    user.setUpdatedAt(LocalDateTime.now());
                                    userRepository.save(user);
                                    System.out.println("Updated existing user with OAuth info: " + email);
                                }
                                
                                // Generate JWT token
                                String jwtToken = jwtTokenProvider.generateToken(email, "USER", firstName, lastName, user.getId(), "google");
                                System.out.println("JWT token generated: " + jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
                                
                                // Redirect to frontend with token
                                String redirectUrl = UriComponentsBuilder.fromUriString(frontendBase)
                                        .path("/oauth/callback")
                                        .queryParam("token", jwtToken)
                                        .build()
                                        .toUriString();
                                response.sendRedirect(redirectUrl);
                                
                            } catch (Exception e) {
                                System.err.println("ERROR in OAuth2 success handler: " + e.getMessage());
                                e.printStackTrace();
                                String redirectUrl = UriComponentsBuilder.fromUriString(frontendBase)
                                        .path("/login")
                                        .queryParam("error", "authentication_failed")
                                        .build()
                                        .toUriString();
                                response.sendRedirect(redirectUrl);
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            System.err.println("OAuth2 authentication failed: " + exception.getMessage());
                            exception.printStackTrace();
                            String frontendBase = normalizedFrontendUrl();
                            String redirectUrl = UriComponentsBuilder.fromUriString(frontendBase)
                                    .path("/login")
                                    .queryParam("error", "oauth_failed")
                                    .build()
                                    .toUriString();
                            response.sendRedirect(redirectUrl);
                        })
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private String normalizedFrontendUrl() {
        String url = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.trim() : "http://localhost:5173";
        int schemeEnd = url.indexOf("://");
        int minLength = schemeEnd >= 0 ? schemeEnd + 3 : 0;
        while (url.endsWith("/") && url.length() > minLength) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
