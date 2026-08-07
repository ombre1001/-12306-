package com.example.railgo.data.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TrainSyncRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
    @AssertTrue(message = "startDate不能晚于endDate，且最多同步31天")
    public boolean isRangeValid() {
        return startDate != null
                && endDate != null
                && !startDate.isAfter(endDate)
                && !endDate.isAfter(startDate.plusDays(30));
    }
}
