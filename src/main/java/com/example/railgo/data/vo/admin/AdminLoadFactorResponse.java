package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdminLoadFactorResponse {
    private Long runId;
    private LocalDate runDate;
    private String trainNo;
    private String seatTypeCode;
    private String seatTypeName;
    private Long totalSegmentCount;
    private Long soldSegmentCount;
    private Long lockedSegmentCount;
    private BigDecimal loadFactor;
}
