package com.example.railgo.data.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalTime;
import java.util.List;

public record AdminTrainStopsRequest(

        @Valid
        @NotEmpty(message = "经停站不能为空")
        @Size(min = 2, message = "一个车次至少需要两个经停站")
        List<StopItem> stops
) {
    public record StopItem(

            @NotNull(message = "车站ID不能为空")
            @Positive(message = "车站ID必须为正整数")
            Long stationId,

            @NotNull(message = "站序不能为空")
            @Positive(message = "站序必须为正整数")
            Integer stopSeq,

            @JsonFormat(pattern = "HH:mm")
            LocalTime arrivalTime,

            @NotNull
            @Min(0)
            @Max(10)
            Integer arrivalDayOffset,

            @JsonFormat(pattern = "HH:mm")
            LocalTime departureTime,

            @NotNull
            @Min(0)
            @Max(10)
            Integer departureDayOffset,

            @NotNull
            @Min(0)
            Integer distanceKm
    ) {
    }
}