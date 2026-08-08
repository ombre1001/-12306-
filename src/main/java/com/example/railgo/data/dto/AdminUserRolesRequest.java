package com.example.railgo.data.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AdminUserRolesRequest(@NotEmpty Set<String> roles) {
}
