package com.example.railgo.data.vo.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareAvailabilityRow {

    private Long runId;

    private Long seatTypeId;

    private String seatTypeCode;

    private String seatTypeName;

    private BigDecimal price;

    private Integer availableCount;
}