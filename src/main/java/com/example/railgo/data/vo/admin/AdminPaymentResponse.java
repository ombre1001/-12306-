package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminPaymentResponse {
    private Long paymentId;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String userPhone;
    private String channel;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
