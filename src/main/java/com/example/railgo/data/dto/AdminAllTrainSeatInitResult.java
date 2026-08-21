package com.example.railgo.data.dto;

import java.util.List;

public record AdminAllTrainSeatInitResult(

        int totalTrainCount,

        int initializedTrainCount,

        int existingStructureSkippedCount,

        int inventoryLockedSkippedCount,

        int generatedCoachCount,

        int generatedSeatCount,

        List<String> inventoryLockedTrainNos
) {
}