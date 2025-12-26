package com.salonplatform.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID userId;
    private String phone;
    private String email;
    private String fullName;
    private String role;
    private String profileImageUrl;
    private String message;
}
