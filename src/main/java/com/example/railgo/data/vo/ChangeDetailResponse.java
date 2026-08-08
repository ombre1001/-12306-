package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChangeDetailResponse {
    private Long changeId;
    private String changeNo;
    private String status;
    private Long oldTicketId;
    private Long newTicketId;
    private Long passengerId;
    private String passengerName;
    private Long oldRunId;
    private String oldTrainNo;
    private String oldSeatTypeCode;
    private String oldCoachNo;
    private String oldSeatNo;
    private Long newRunId;
    private String newTrainNo;
    private String newSeatTypeCode;
    private String newCoachNo;
    private String newSeatNo;
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    private LocalDateTime newDepartureDateTime;
    private LocalDateTime newArrivalDateTime;
    private BigDecimal originalAmount;
    private BigDecimal newAmount;
    private BigDecimal differenceAmount;
    private String differenceType;
    private LocalDateTime expireAt;
    private Long remainingSeconds;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
}
