package com.salonplatform.dto;

import lombok.*;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSalonRequest {
    private String name;
    private String description;
    private String address;
    private String phone;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Integer onlineBookingPercentage;
}