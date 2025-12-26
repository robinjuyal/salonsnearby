package com.salonplatform.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private Boolean isActive;
}
