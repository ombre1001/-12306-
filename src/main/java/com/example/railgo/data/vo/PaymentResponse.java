package com.example.railgo.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private Long paymentId;
    private String paymentNo;
    private String paymentStatus;
    private String channel;
    private BigDecimal amount;
    private LocalDateTime paidAt;
}
