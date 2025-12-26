package com.salonplatform.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID bookingId;
    private String customerName;
    private String customerPhone;
    private String salonName;
    private String serviceName;
    private String barberName;
    private LocalDateTime estimatedStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Integer estimatedDurationMinutes;
    private Integer queuePosition;
    private BigDecimal amount;
    private String status;
    private String paymentStatus;
    private String bookingType;
    private String paymentOrderId;
    private String paymentKey;
}
