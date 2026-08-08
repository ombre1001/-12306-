package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MyTicketDetailResponse {
    /** 实际对应 order_item.id。 */
    private Long ticketId;
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private String ticketStatus;
    private Long passengerId;
    private String passengerName;
    private String idType;
    private Long runId;
    private LocalDate runDate;
    private String runStatus;
    private String trainNo;
    private Long fromStationId;
    private String fromStationName;
    private Integer fromSeq;
    private Long toStationId;
    private String toStationName;
    private Integer toSeq;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private Long seatTypeId;
    private String seatTypeCode;
    private String seatTypeName;
    private Long seatId;
    private String coachNo;
    private String seatNo;
    private BigDecimal price;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
