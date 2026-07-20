package com.example.railgo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.AuthRefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AuthRefreshTokenMapper
        extends BaseMapper<AuthRefreshToken> {

    @Update("""
            UPDATE auth_refresh_token
            SET revoked_at = #{now}
            WHERE user_id = #{userId}
              AND revoked_at IS NULL
            """)
    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
}
