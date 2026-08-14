package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminInventoryBatchInitRequest(

        @Positive(message = "车次ID必须为正整数")
        Long trainId,

        @NotNull(message = "开始日期不能为空")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @NotNull(message = "结束日期不能为空")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate
) {
}