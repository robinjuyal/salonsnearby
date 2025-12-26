package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private UUID salonId;
    private Boolean isOpen;
    private Boolean acceptsOnlineBooking;
    private Long currentQueue;
    private LocalDateTime nextAvailableTime;
    private Integer estimatedWaitMinutes;
    private Integer availableBarbers;
}
