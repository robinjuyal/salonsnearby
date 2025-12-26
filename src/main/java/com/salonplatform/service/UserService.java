package com.salonplatform.service;

import com.salonplatform.dto.NotificationResponse;
import com.salonplatform.dto.UpdateProfileRequest;
import com.salonplatform.dto.UserProfileResponse;
import com.salonplatform.entity.Notification;
import com.salonplatform.entity.User;
import com.salonplatform.exception.BusinessException;
import com.salonplatform.exception.ResourceNotFoundException;
import com.salonplatform.repository.NotificationRepository;
import com.salonplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
                throw new BusinessException("Email is already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        user = userRepository.save(user);
        return toUserProfileResponse(user);
    }

    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId, int limit) {
        return notificationRepository.findRecentNotifications(userId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .profileImageUrl(user.getProfileImageUrl())
                .isVerified(user.getIsVerified())
                .noShowCount(user.getNoShowCount())
                .totalBookings(user.getTotalBookings())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .bookingId(notification.getBooking() != null ? notification.getBooking().getId() : null)
                .build();
    }
}