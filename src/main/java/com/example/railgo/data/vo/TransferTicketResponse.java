package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferTicketResponse {

    private Long transferStationId;

    private String transferStationCode;

    private String transferStation;

    private DirectTicketResponse firstLeg;

    private DirectTicketResponse secondLeg;

    private Integer waitMinutes;

    private Integer totalDurationMinutes;

    private BigDecimal minimumTotalPrice;
}