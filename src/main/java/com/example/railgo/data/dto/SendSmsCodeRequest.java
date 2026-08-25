package com.example.railgo.data.dto;

import com.example.railgo.config.SmsPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SendSmsCodeRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotNull(message = "验证码用途不能为空")
        SmsPurpose purpose
) {
}
