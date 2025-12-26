package com.salonplatform.dto;

import com.salonplatform.enums.ServiceCategory;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRequest {
    @NotBlank(message = "Service name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Duration is required")
    @Min(value = 10, message = "Duration must be at least 10 minutes")
    private Integer durationMinutes;

    private ServiceCategory category;
}
