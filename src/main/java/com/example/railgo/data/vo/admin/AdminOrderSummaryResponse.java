package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminOrderSummaryResponse {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String userPhone;
    private String userNickname;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDate firstTravelDate;
    private String trainNos;
    private String fromStationName;
    private String toStationName;
    private Integer passengerCount;
    private Integer ticketCount;
}
