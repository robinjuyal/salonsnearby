package com.salonplatform.service;

import com.salonplatform.dto.AuthResponse;
import com.salonplatform.dto.LoginRequest;
import com.salonplatform.dto.SignUpRequest;
import com.salonplatform.entity.User;
import com.salonplatform.enums.UserRole;
import com.salonplatform.exception.BusinessException;
import com.salonplatform.repository.UserRepository;
import com.salonplatform.security.JwtTokenProvider;
import com.salonplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    /**
     * Register new user
     */
    @Transactional
    public AuthResponse registerUser(SignUpRequest request) {
        log.info("Registering new user with phone: {}", request.getPhone());

        // Check if user already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone number already registered");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }

        // Validate password strength
        validatePassword(request.getPassword());

        // Create new user
        User user = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(determineUserRole(request.getRole()))
                .isVerified(false)
                .isActive(true)
                .noShowCount(0)
                .totalBookings(0)
                .build();

        user = userRepository.save(user);

        // Create UserPrincipal and generate token
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        String token = tokenProvider.generateToken(authentication);

        log.info("User registered successfully: {}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .message("Registration successful")
                .build();
    }

    /**
     * Login user
     */
    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request) {
        log.info("Login attempt for phone: {}", request.getPhone());

        // Check if user exists
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException("Invalid phone number or password"));

        if (!user.getIsActive()) {
            throw new BusinessException("Account is deactivated. Please contact support");
        }

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getPhone(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate token
        String token = tokenProvider.generateToken(authentication);

        log.info("User logged in successfully: {}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .profileImageUrl(user.getProfileImageUrl())
                .message("Login successful")
                .build();
    }

    /**
     * Verify user account (placeholder for OTP verification)
     */
    @Transactional
    public void verifyAccount(UUID userId, String verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // TODO: Verify OTP code when SMS service is integrated
        // For now, just mark as verified
        user.setIsVerified(true);
        userRepository.save(user);

        log.info("User account verified: {}", userId);
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Validate new password
        validatePassword(newPassword);

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    /**
     * Request password reset
     */
    @Transactional
    public void requestPasswordReset(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException("No account found with this phone number"));

        // TODO: Generate reset token and send via SMS
        // For now, just log
        log.info("Password reset requested for user: {}", user.getId());

        // In production, you would:
        // 1. Generate a unique reset token
        // 2. Store it with expiration time
        // 3. Send SMS with reset link
    }

    /**
     * Reset password with token
     */
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        // TODO: Validate reset token
        // For now, this is a placeholder

        validatePassword(newPassword);

        // Find user by reset token (you'd need to add this field to User entity)
        // Update password

        log.info("Password reset completed");
    }

    /**
     * Logout (client-side token removal, but we can blacklist if needed)
     */
    public void logout(String token) {
        // TODO: If implementing token blacklist with Redis
        // Add token to blacklist until expiration

        log.info("User logged out");
    }

    /**
     * Refresh JWT token
     */
    public AuthResponse refreshToken(String oldToken) {
        if (!tokenProvider.validateToken(oldToken)) {
            throw new BusinessException("Invalid or expired token");
        }

        String userId = tokenProvider.getUserIdFromToken(oldToken);
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("User not found"));

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        String newToken = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(newToken)
                .userId(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .message("Token refreshed")
                .build();
    }

    // Helper methods

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters long");
        }

        // Add more password rules as needed
        if (!password.matches(".*[0-9].*")) {
            throw new BusinessException("Password must contain at least one digit");
        }
    }

    private UserRole determineUserRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isEmpty()) {
            return UserRole.CUSTOMER; // Default role
        }

        try {
            UserRole role = UserRole.valueOf(requestedRole.toUpperCase());

            // Prevent self-registration as ADMIN
            if (role == UserRole.ADMIN) {
                log.warn("Attempt to register as ADMIN, defaulting to CUSTOMER");
                return UserRole.CUSTOMER;
            }

            return role;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role requested: {}, defaulting to CUSTOMER", requestedRole);
            return UserRole.CUSTOMER;
        }
    }

    /**
     * Check if phone number is available
     */
    public boolean isPhoneAvailable(String phone) {
        return !userRepository.existsByPhone(phone);
    }

    /**
     * Check if email is available
     */
    public boolean isEmailAvailable(String email) {
        if (email == null || email.isEmpty()) {
            return true;
        }
        return !userRepository.existsByEmail(email);
    }
}