package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminChangeResponse {
    private Long changeId;
    private String changeNo;
    private Long userId;
    private String userPhone;
    private Long orderId;
    private String orderNo;
    private Long oldOrderItemId;
    private Long newOrderItemId;
    private String oldTrainNo;
    private String newTrainNo;
    private BigDecimal originalAmount;
    private BigDecimal newAmount;
    private BigDecimal differenceAmount;
    private String differenceType;
    private String status;
    private LocalDateTime expireAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
}
