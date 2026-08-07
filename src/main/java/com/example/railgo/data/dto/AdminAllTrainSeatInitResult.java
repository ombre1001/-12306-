package com.example.railgo.data.dto;

public record AdminAllTrainSeatInitResult(

        int totalTrainCount,

        int initializedTrainCount,

        int skippedTrainCount,

        int generatedCoachCount,

        int generatedSeatCount
) {
}