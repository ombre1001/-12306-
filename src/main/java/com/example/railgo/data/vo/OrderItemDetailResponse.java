package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderItemDetailResponse {

    private Long orderItemId;
    private Long passengerId;
    private String passengerName;
    private Long runId;
    private LocalDate runDate;
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
    private String status;
}
