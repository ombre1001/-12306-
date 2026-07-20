package com.example.railgo.security;


import java.time.Instant;
import java.util.List;

public record TokenClaims(
        Long userId,
        String phone,
        List<String> roles,
        String type,
        String jti,
        Instant expiresAt
) {
}
