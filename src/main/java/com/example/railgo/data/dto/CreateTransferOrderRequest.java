package com.example.railgo.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateTransferOrderRequest {

    @Valid
    @NotNull(message = "第一程不能为空")
    private TransferOrderLegRequest firstLeg;

    @Valid
    @NotNull(message = "第二程不能为空")
    private TransferOrderLegRequest secondLeg;

    @Valid
    @NotEmpty(message = "至少选择一名乘车人")
    @Size(max = 5, message = "一次最多购买5名乘车人的换乘票")
    private List<TransferOrderPassengerRequest> items;

    @NotBlank(message = "clientRequestId不能为空")
    @Size(max = 64, message = "clientRequestId长度不能超过64")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "clientRequestId必须是标准UUID"
    )
    private String clientRequestId;
}
