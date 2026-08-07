package com.example.railgo.data.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RunStopResponse {

    private Long runId;

    private LocalDate runDate;

    private String trainNo;

    private Integer stopSeq;

    private Long stationId;

    private String stationCode;

    private String stationName;

    private LocalDateTime arrivalDateTime;

    private LocalDateTime departureDateTime;

    /**
     * 停站分钟数。
     * 始发站和终到站为 null。
     */
    private Integer stopMinutes;

    private Integer distanceKm;

    private Boolean origin;

    private Boolean terminal;
}