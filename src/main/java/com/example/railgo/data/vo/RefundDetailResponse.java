package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundDetailResponse {
    private Long refundId;
    private String refundNo;
    private String status;
    private BigDecimal amount;
    private String paymentNo;
    private String channel;
    private Long returnId;
    private String returnNo;
    private Long ticketId;
    private Long orderId;
    private String orderNo;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
