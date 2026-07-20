package com.example.railgo.data.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String phone,
        String nickname,
        String email,
        String status,
        List<String> roles,
        LocalDateTime createdAt
) {
}
