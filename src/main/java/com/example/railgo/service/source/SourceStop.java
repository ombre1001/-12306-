package com.example.railgo.service.source;

import java.time.LocalTime;

public record SourceStop(
        int stopSeq,
        String stationName,
        LocalTime arrivalTime,
        int arrivalDayOffset,
        LocalTime departureTime,
        int departureDayOffset
) {
}