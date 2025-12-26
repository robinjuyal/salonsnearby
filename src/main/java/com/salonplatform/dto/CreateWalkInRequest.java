package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalkInRequest {
    @NotNull(message = "Salon ID is required")
    private UUID salonId;

    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    private String customerName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String customerPhone;
}
