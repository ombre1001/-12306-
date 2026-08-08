package com.example.railgo.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminCreateRequest(
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误") String phone,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 50) String nickname,
        @Email @Size(max = 100) String email,
        @NotEmpty Set<String> roles
) {
}
