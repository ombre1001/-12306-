package com.example.railgo.data.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TransferOrderPassengerRequest {

    @NotNull(message = "乘车人ID不能为空")
    @Positive(message = "乘车人ID必须大于0")
    private Long passengerId;

    @NotBlank(message = "第一程席别不能为空")
    @Size(max = 32, message = "第一程席别编码长度不能超过32")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "第一程席别编码格式不合法")
    private String firstSeatTypeCode;

    @NotBlank(message = "第二程席别不能为空")
    @Size(max = 32, message = "第二程席别编码长度不能超过32")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "第二程席别编码格式不合法")
    private String secondSeatTypeCode;

    @Pattern(
            regexp = "(?i)WINDOW|AISLE|NONE",
            message = "第一程座位偏好只能是WINDOW、AISLE或NONE"
    )
    private String firstSeatPreference = "NONE";

    @Pattern(
            regexp = "(?i)WINDOW|AISLE|NONE",
            message = "第二程座位偏好只能是WINDOW、AISLE或NONE"
    )
    private String secondSeatPreference = "NONE";
}
