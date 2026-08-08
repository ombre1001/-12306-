package com.example.railgo.data.vo.admin;

import lombok.Data;

@Data
public class AdminInventoryOccupationResponse {
    private Long orderItemId;
    private Long runId;
    private String trainNo;
    private String coachNo;
    private String seatNo;
    private Integer fromSeq;
    private Integer toSeq;
    private Integer occupiedSegmentCount;
    private String inventoryStatuses;
}
