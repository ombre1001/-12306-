package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketReturnRequest {

    @NotBlank(message = "客户端请求号不能为空")
    @Size(max = 64, message = "客户端请求号长度不能超过64")
    private String clientRequestId;
}
