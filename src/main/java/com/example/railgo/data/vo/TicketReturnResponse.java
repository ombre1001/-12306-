package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketReturnResponse {
    private Long returnId;
    private String returnNo;
    private Long orderId;
    private String orderNo;
    private Long ticketId;
    private String passengerName;
    private String trainNo;
    private String fromStationName;
    private String toStationName;
    private LocalDateTime departureDateTime;
    private BigDecimal ticketAmount;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal refundAmount;
    private String returnStatus;
    private Long refundId;
    private String refundNo;
    private String refundStatus;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
