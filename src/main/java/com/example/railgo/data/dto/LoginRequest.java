package com.example.railgo.data.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "请输入邮箱或手机号")
        @Size(max = 254, message = "账号长度不正确")
        String account,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
