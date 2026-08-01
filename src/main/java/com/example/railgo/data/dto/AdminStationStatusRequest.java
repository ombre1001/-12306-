package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminStationStatusRequest(

        @NotBlank(message = "车站状态不能为空")
        @Pattern(
                regexp = "ACTIVE|INACTIVE",
                message = "车站状态只能是ACTIVE或INACTIVE"
        )
        String status
) {
}