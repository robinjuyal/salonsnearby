package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private String category;
    private Boolean isActive;
}
