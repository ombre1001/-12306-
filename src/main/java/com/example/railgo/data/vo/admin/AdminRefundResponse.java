package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminRefundResponse {
    private Long refundId;
    private String refundNo;
    private Long returnId;
    private String returnNo;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long userId;
    private String userPhone;
    private BigDecimal ticketAmount;
    private BigDecimal feeAmount;
    private BigDecimal amount;
    private String status;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
