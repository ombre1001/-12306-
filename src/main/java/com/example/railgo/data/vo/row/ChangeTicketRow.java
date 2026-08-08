package com.example.railgo.data.vo.row;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChangeTicketRow {
    private Long ticketId;
    private Long orderId;
    private Long userId;
    private Long passengerId;
    private String passengerName;
    private Long runId;
    private Long trainId;
    private String trainNo;
    private Long fromStationId;
    private Long toStationId;
    private Integer fromSeq;
    private Integer toSeq;
    private Long seatTypeId;
    private String seatTypeCode;
    private Long seatId;
    private String coachNo;
    private String seatNo;
    private BigDecimal price;
    private String status;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
}
