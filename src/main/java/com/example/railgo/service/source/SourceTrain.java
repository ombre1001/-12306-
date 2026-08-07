package com.example.railgo.service.source;

public record SourceTrain(
        String sourceTrainCode,
        String trainNo,
        String trainType,
        String fromStationCode,
        String toStationCode
) {
}
