package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RunFareQueryRequest {

    @NotNull(message = "出发站不能为空")
    private Long fromStationId;

    @NotNull(message = "到达站不能为空")
    private Long toStationId;
}