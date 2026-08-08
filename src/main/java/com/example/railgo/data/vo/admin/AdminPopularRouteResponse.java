package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminPopularRouteResponse {
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    private Long ticketCount;
    private Long passengerCount;
    private BigDecimal salesAmount;
}
