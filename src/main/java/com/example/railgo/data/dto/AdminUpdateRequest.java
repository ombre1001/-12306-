package com.example.railgo.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateRequest(
        @NotBlank @Size(max = 50) String nickname,
        @Email @Size(max = 100) String email
) {
}
