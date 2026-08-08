package com.example.railgo.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketReturnPreviewResponse {
    private Long ticketId;
    private Long orderId;
    private String orderNo;
    private String passengerName;
    private String trainNo;
    private String fromStationName;
    private String toStationName;
    private LocalDateTime departureDateTime;
    private BigDecimal ticketAmount;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal refundAmount;
}
