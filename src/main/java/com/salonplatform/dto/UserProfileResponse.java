package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String phone;
    private String email;
    private String fullName;
    private String role;
    private String profileImageUrl;
    private Boolean isVerified;
    private Integer noShowCount;
    private Integer totalBookings;
    private LocalDateTime createdAt;
}
