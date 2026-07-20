package com.example.railgo.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_refresh_token")
public class AuthRefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tokenJti;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;
}