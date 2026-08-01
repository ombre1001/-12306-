package com.example.railgo.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LockedTicketResponse {

    private Long orderItemId;

    private Long passengerId;

    private String seatTypeCode;

    private String seatTypeName;

    private String coachNo;

    private String seatNo;

    private BigDecimal price;
}