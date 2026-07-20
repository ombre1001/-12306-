package com.example.railgo.data.vo;


import com.example.railgo.data.vo.UserProfileResponse;

public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfileResponse user
) {
}