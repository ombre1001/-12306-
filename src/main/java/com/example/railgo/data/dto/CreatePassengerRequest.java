package com.example.railgo.data.dto;

import com.example.railgo.data.enums.IdType;
import com.example.railgo.data.enums.PassengerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "新增常用乘车人请求")
public record CreatePassengerRequest(

        @NotBlank(message = "乘车人姓名不能为空")
        @Size(max = 50, message = "乘车人姓名不能超过50个字符")
        @Schema(example = "张三")
        String name,

        @NotNull(message = "证件类型不能为空")
        @Schema(example = "ID_CARD")
        IdType idType,

        @NotBlank(message = "证件号码不能为空")
        @Size(max = 50, message = "证件号码不能超过50个字符")
        @Schema(example = "11010519491231002X")
        String idNo,

        @NotNull(message = "旅客类型不能为空")
        @Schema(example = "ADULT")
        PassengerType passengerType,

        @Pattern(
                regexp = "^$|^1[3-9]\\d{9}$",
                message = "手机号格式不正确"
        )
        @Schema(example = "13800000000")
        String phone

) {
}