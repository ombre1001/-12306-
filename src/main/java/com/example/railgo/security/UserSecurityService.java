package com.example.railgo.security;


import com.example.railgo.data.po.SysUser;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;

import com.example.railgo.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSecurityService {

    private final SysUserMapper userMapper;

    public RailUserPrincipal loadPrincipal(Long userId) {

        SysUser user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }

        return new RailUserPrincipal(
                user.getId(),
                user.getPhone(),
                user.getPasswordHash(),
                "ENABLED".equals(user.getStatus()),
                userMapper.selectRoleCodesByUserId(
                        userId
                ),
                userMapper.selectPermissionCodesByUserId(userId)
        );
    }
}
