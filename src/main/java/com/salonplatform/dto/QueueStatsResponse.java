package com.salonplatform.dto;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatsResponse {
    private Long totalInQueue;
    private Long currentlyServing;
    private Integer estimatedTotalWaitTime;
    private Long averageWaitTime;
}