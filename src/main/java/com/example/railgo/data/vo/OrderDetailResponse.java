package com.example.railgo.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {

    private Long orderId;
    private String orderNo;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime expireAt;
    private Long remainingSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDetailResponse> items;
}
