package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserStatusRequest(@NotBlank String status) {
}
