package com.example.railgo.data.vo.row;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransferCandidateRow {

    private Long transferStationId;
    private String transferStationCode;
    private String transferStation;

    private Long firstRunId;
    private String firstTrainNo;
    private String firstTrainType;
    private String firstOriginStation;
    private String firstTerminalStation;
    private Long firstFromStationId;
    private String firstFromStation;
    private String firstToStation;
    private Integer firstFromSeq;
    private Integer firstToSeq;
    private LocalDateTime firstDepartureDateTime;
    private LocalDateTime firstArrivalDateTime;
    private Integer firstDurationMinutes;

    private Long secondRunId;
    private String secondTrainNo;
    private String secondTrainType;
    private String secondOriginStation;
    private String secondTerminalStation;
    private String secondFromStation;
    private Long secondToStationId;
    private String secondToStation;
    private Integer secondFromSeq;
    private Integer secondToSeq;
    private LocalDateTime secondDepartureDateTime;
    private LocalDateTime secondArrivalDateTime;
    private Integer secondDurationMinutes;

    private Integer waitMinutes;
    private Integer totalDurationMinutes;
}