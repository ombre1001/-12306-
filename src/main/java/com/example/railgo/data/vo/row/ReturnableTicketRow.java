package com.example.railgo.data.vo.row;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReturnableTicketRow {
    private Long ticketId;
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private Long userId;
    private String ticketStatus;
    private String passengerName;
    private String trainNo;
    private String fromStationName;
    private String toStationName;
    private Integer fromSeq;
    private Integer toSeq;
    private BigDecimal price;
    private LocalDateTime departureDateTime;
}
