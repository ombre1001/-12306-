package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderPassengerRequest {

    @NotNull(message = "乘车人ID不能为空")
    private Long passengerId;

    @NotBlank(message = "席别不能为空")
    private String seatTypeCode;

    /**
     * WINDOW：靠窗
     * AISLE：靠过道
     * NONE：无偏好
     */
    private String seatPreference = "NONE";
}