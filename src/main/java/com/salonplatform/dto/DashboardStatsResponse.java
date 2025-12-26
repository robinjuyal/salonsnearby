package com.salonplatform.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalBookingsToday;
    private Long completedBookingsToday;
    private Long cancelledBookingsToday;
    private Long noShowsToday;
    private Long currentQueue;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;
    private Double averageRating;
    private Integer totalReviews;
}

