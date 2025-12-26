package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberResponse {
    private UUID id;
    private String name;
    private String phone;
    private String specialization;
    private Integer experienceYears;
    private BigDecimal rating;
    private Integer totalServices;
    private Boolean isAvailable;
}
