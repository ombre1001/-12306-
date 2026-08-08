package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MyTicketResponse {
    /** 实际对应 order_item.id，可直接用于退票、改签与详情接口。 */
    private Long ticketId;
    private Long orderId;
    private String orderNo;
    private String ticketStatus;
    private Long passengerId;
    private String passengerName;
    private Long runId;
    private String trainNo;
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private String seatTypeCode;
    private String seatTypeName;
    private String coachNo;
    private String seatNo;
    private BigDecimal price;
    private LocalDateTime issuedAt;
}
