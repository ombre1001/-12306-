package com.example.railgo.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(
                min = 1,
                max = 50,
                message = "昵称长度必须为1～50个字符"
        )
        String nickname,

        @Email(message = "邮箱格式不正确")
        @Size(
                max = 100,
                message = "邮箱不能超过100个字符"
        )
        String email,

        @Pattern(
                regexp = "^1[3-9]\\d{9}$",
                message = "手机号格式不正确"
        )
        String phone,

        @Pattern(
                regexp = "^\\d{6}$",
                message = "验证码必须为6位数字"
        )
        String verificationCode
) {
}
