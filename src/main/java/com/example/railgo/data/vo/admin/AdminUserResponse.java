package com.example.railgo.data.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserResponse {
    private Long id;
    private String phone;
    private String nickname;
    private String email;
    private String status;
    private List<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
