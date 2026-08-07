package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransferOrderLegRequest {

    @NotNull(message = "运行实例ID不能为空")
    @Positive(message = "运行实例ID必须大于0")
    private Long runId;

    @NotNull(message = "出发站不能为空")
    @Positive(message = "出发站ID必须大于0")
    private Long fromStationId;

    @NotNull(message = "到达站不能为空")
    @Positive(message = "到达站ID必须大于0")
    private Long toStationId;
}
