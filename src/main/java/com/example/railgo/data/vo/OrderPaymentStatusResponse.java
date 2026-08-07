package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderPaymentStatusResponse {

    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private String paymentNo;
    private String paymentStatus;
    private String channel;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
