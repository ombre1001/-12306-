package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderSummaryResponse {

    private Long orderId;
    private String orderNo;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private Long remainingSeconds;
    private LocalDate firstTravelDate;
    private String trainNos;
    private String fromStationName;
    private String toStationName;
    private Integer passengerCount;
    private Integer ticketCount;
}
