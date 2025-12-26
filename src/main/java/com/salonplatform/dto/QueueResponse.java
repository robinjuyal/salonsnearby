package com.salonplatform.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponse {
    private UUID queueId;
    private UUID bookingId;
    private String customerName;
    private String serviceName;
    private String barberName;
    private Integer position;
    private Integer estimatedWaitMinutes;
    private LocalDateTime estimatedStartTime;
    private String status;
    private String bookingType;
    private Boolean isPaid;
    private LocalDateTime addedAt;
}
