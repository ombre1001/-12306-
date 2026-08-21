package com.example.railgo.data.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DirectTicketResponse {

    private Long runId;

    private String trainNo;

    private String trainType;

    private String originStation;

    private String terminalStation;

    private Long fromStationId;

    private String fromStation;

    private Long toStationId;

    private String toStation;

    private Integer fromSeq;

    private Integer toSeq;

    private LocalDateTime departureDateTime;

    private LocalDateTime arrivalDateTime;

    private Integer durationMinutes;

    private List<FareAvailabilityResponse> fares =
            new ArrayList<>();
}