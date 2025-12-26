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
public class SalonDetailResponse {
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
    private Boolean acceptsOnlineBooking;
    private BigDecimal rating;
    private Integer totalReviews;
    private List<ServiceResponse> services;
    private List<BarberResponse> barbers;
    private List<String> images;
    private Long currentQueue;
    private LocalDateTime nextAvailableTime;
    private String subscriptionTier;
}
