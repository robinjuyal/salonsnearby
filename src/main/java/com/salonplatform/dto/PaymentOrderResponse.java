package com.salonplatform.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    private String orderId;
    private String key;
    private BigDecimal amount;
    private String currency;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String description;
}
