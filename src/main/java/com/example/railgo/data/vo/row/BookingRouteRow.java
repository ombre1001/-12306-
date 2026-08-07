package com.example.railgo.data.vo.row;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRouteRow {

    private Long runId;

    private Long trainId;

    private String trainNo;

    private String saleStatus;

    private Integer inventoryInitialized;

    private Long fromStationId;

    private Long toStationId;

    private Integer fromSeq;

    private Integer toSeq;

    private LocalDateTime departureDateTime;

    private LocalDateTime arrivalDateTime;
}
