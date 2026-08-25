package com.example.railgo.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱长度不能超过254个字符")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(
                min = 8,
                max = 64,
                message = "密码长度必须为8～64位"
        )
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "密码必须同时包含字母和数字"
        )
        String password,

        @NotBlank(message = "验证码不能为空")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "验证码必须为6位数字"
        )
        String verificationCode,

        @Size(
                max = 50,
                message = "昵称不能超过50个字符"
        )
        String nickname
) {
}
