package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.LoginRequest;
import com.example.finsight_backend.dto.RegisterRequest;
import com.example.finsight_backend.dto.UserResponse;
import com.example.finsight_backend.dto.UpdateUserRequest;
import com.example.finsight_backend.dto.ChangePasswordConfirmRequest;
import com.example.finsight_backend.dto.ForgotPasswordRequest;
import com.example.finsight_backend.dto.ResetPasswordRequest;
import com.example.finsight_backend.entity.Role;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.UserRepository;
import com.example.finsight_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;

    public void initiateRegistration(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with this email already exists");
        }

        // Send OTP for email verification
        otpService.sendOtp(request.getEmail(), request.getFirstName());
        log.info("Registration initiated for email: {}", request.getEmail());
    }

    public UserResponse completeRegistration(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with this email already exists");
        }

        // Create new user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        return mapToUserResponse(savedUser);
    }

    public String login(LoginRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Get user details for token generation
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token with user details
        String token = jwtTokenProvider.generateToken(
                user.getEmail(), 
                user.getRole().name(), 
                user.getFirstName(), 
                user.getLastName(),
                user.getId(),
                user.getProvider() != null ? user.getProvider() : "local"
        );
        log.info("User logged in: {}", request.getEmail());

        return token;
    }

    public String generateTokenForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return jwtTokenProvider.generateToken(
                email, 
                user.getRole().name(), 
                user.getFirstName(), 
                user.getLastName(),
                user.getId(),
                user.getProvider() != null ? user.getProvider() : "local"
        );
    }

    public void initiateForgotPassword(ForgotPasswordRequest request) {
        // Check if user exists
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User with this email does not exist"));

        // Send OTP for password reset
        otpService.sendOtp(request.getEmail(), user.getFirstName(), "password_reset");
        log.info("Forgot password initiated for email: {}", request.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Verify OTP
        boolean otpValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!otpValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Find user and update password
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password reset successfully for email: {}", request.getEmail());
    }

    public void initiateChangePassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with this email does not exist"));
        otpService.sendOtp(email, user.getFirstName(), "password_change");
        log.info("Change password OTP sent for email: {}", email);
    }

    public void confirmChangePassword(String email, ChangePasswordConfirmRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        boolean otpValid = otpService.verifyOtp(email, request.getOtp());
        if (!otpValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for email: {}", email);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isSelf = target.getEmail().equalsIgnoreCase(requesterEmail);
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        if (!isSelf && !isAdmin) {
            throw new RuntimeException("Forbidden");
        }

        target.setFirstName(request.getFirstName());
        target.setLastName(request.getLastName());
        User saved = userRepository.save(target);
        return mapToUserResponse(saved);
    }

    public void deleteUser(Long id, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isSelf = target.getEmail().equalsIgnoreCase(requesterEmail);
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        if (!isSelf && !isAdmin) {
            throw new RuntimeException("Forbidden");
        }

        userRepository.delete(target);
    }
}
