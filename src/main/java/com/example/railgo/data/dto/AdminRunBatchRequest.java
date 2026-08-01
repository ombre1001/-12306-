package com.example.railgo.data.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminRunBatchRequest(

        @NotNull(message = "车次ID不能为空")
        @Positive
        Long trainId,

        @NotNull(message = "开始日期不能为空")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @NotNull(message = "结束日期不能为空")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        Boolean initializeInventory
) {
}