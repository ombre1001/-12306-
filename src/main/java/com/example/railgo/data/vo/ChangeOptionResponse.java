package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChangeOptionResponse {
    private Long runId;
    private String trainNo;
    private String trainType;
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private String seatTypeCode;
    private String seatTypeName;
    private BigDecimal price;
    private Integer availableCount;
}
