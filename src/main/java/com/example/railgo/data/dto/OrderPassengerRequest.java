package com.example.railgo.data.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderPassengerRequest {

    @NotNull(message = "乘车人ID不能为空")
    @Positive(message = "乘车人ID必须大于0")
    private Long passengerId;

    @NotBlank(message = "席别不能为空")
    @Size(max = 32, message = "席别编码长度不能超过32")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "席别编码格式不合法")
    private String seatTypeCode;

    /**
     * WINDOW：靠窗
     * AISLE：靠过道
     * NONE：无偏好
     */
    @Pattern(
            regexp = "(?i)WINDOW|AISLE|NONE",
            message = "座位偏好只能是WINDOW、AISLE或NONE"
    )
    private String seatPreference = "NONE";
}
