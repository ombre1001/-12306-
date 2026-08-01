package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminRunStatusRequest(

        @NotBlank(message = "运行状态不能为空")
        @Pattern(
                regexp = "DRAFT|NOT_ON_SALE|ON_SALE|OFF_SALE|CANCELLED",
                message = "运行状态不合法"
        )
        String saleStatus
) {
}