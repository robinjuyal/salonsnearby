package com.salonplatform.dto;

import lombok.*;
import java.time.LocalDateTime;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private UUID bookingId;
}
