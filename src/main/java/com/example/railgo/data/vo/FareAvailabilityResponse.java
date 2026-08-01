package com.example.railgo.data.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareAvailabilityResponse {

    private String seatTypeCode;

    private String seatTypeName;

    private BigDecimal price;

    private Integer availableCount;
}