package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private String customerName;
    private String salonName;
    private String barberName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}