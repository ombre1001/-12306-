package com.example.railgo.data.vo;

import lombok.Data;

@Data
public class InventorySummaryResponse {

    private Long seatTypeId;

    private String seatTypeCode;

    private String seatTypeName;

    private Integer totalSegmentCount;

    private Integer availableSegmentCount;

    private Integer lockedSegmentCount;

    private Integer soldSegmentCount;
}