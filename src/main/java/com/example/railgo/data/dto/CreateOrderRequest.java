package com.example.railgo.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotNull(message = "运行实例ID不能为空")
    private Long runId;

    @NotNull(message = "出发站不能为空")
    private Long fromStationId;

    @NotNull(message = "到达站不能为空")
    private Long toStationId;

    @Valid
    @NotEmpty(message = "至少选择一名乘车人")
    @Size(max = 5, message = "一次最多购买5张车票")
    private List<OrderPassengerRequest> items;

    @NotBlank(message = "clientRequestId不能为空")
    @Size(max = 64)
    private String clientRequestId;
}