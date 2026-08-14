package com.example.railgo.data.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminInventoryBatchInitResult(

        int totalRunCount,

        int initializedRunCount,

        int skippedRunCount,

        int failedRunCount,

        List<FailureItem> failures
) {
    public record FailureItem(
            Long runId,
            Long trainId,
            String trainNo,
            LocalDate runDate,
            String reason
    ) {
    }
}