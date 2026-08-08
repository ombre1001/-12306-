package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePreviewRequest {

    @NotNull(message = "新运行实例不能为空")
    @Positive(message = "新运行实例ID必须大于0")
    private Long newRunId;

    @NotNull(message = "出发站不能为空")
    @Positive(message = "出发站ID必须大于0")
    private Long fromStationId;

    @NotNull(message = "到达站不能为空")
    @Positive(message = "到达站ID必须大于0")
    private Long toStationId;

    @NotBlank(message = "席别不能为空")
    @Size(max = 32, message = "席别编码不能超过32个字符")
    private String seatTypeCode;

    @Size(max = 16, message = "座位偏好不能超过16个字符")
    private String seatPreference;

    @NotBlank(message = "客户端请求ID不能为空")
    @Size(max = 64, message = "客户端请求ID不能超过64个字符")
    private String clientRequestId;
}
