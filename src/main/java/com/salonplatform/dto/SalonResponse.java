package com.salonplatform.dto;

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
public class SalonResponse {
    private UUID id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Boolean isOpen;
    private BigDecimal rating;
    private Integer totalReviews;
    private Long currentQueue;
    private LocalDateTime nextAvailableTime;
    private Integer estimatedWaitMinutes;
    private Double distance; // in km
}
