package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    @NotNull(message = "Salon ID is required")
    private UUID salonId;

    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    private UUID barberId; // Optional - system will assign if not provided

    private String specialRequests;
}

