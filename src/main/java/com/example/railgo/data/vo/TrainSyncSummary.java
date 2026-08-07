package com.example.railgo.data.vo;

import java.time.LocalDate;

public record TrainSyncSummary(
        LocalDate startDate,
        LocalDate endDate,
        int successDays,
        int failedDays,
        int trainCount,
        int stopCount
) {
}
