package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatsResponse {
    private Integer totalBookings;
    private Integer onlineBookings;
    private Integer walkInBookings;
    private Integer completedBookings;
    private Integer cancelledBookings;
    private Integer noShows;
    private Double completionRate;
    private Double noShowRate;
}