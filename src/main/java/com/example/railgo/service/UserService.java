package com.example.railgo.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.dto.UpdateProfileRequest;
import com.example.railgo.data.po.SysUser;
import com.example.railgo.data.vo.UserProfileResponse;
import com.example.railgo.mapper.AuthRefreshTokenMapper;
import com.example.railgo.mapper.SysUserMapper;
import com.example.railgo.data.dto.ChangePasswordRequest;
import com.example.railgo.data.dto.UpdateProfileRequest;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;

    private final AuthRefreshTokenMapper
            refreshTokenMapper;

    private final PasswordEncoder passwordEncoder;

    private final VerificationCodeService
            verificationCodeService;

    public UserProfileResponse getProfile(Long userId) {
        return toProfile(requireUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(
            Long userId,
            UpdateProfileRequest request) {

        SysUser user = requireUser(userId);

        if (request.phone() != null
                && !request.phone().equals(
                user.getPhone()
        )) {

            verificationCodeService.verify(
                    request.phone(),
                    request.verificationCode()
            );

            Long count = userMapper.selectCount(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(
                                    SysUser::getPhone,
                                    request.phone()
                            )
                            .ne(
                                    SysUser::getId,
                                    userId
                            )
            );

            if (count > 0) {
                throw new BusinessException(
                        ErrorCode.PHONE_EXISTS
                );
            }

            user.setPhone(request.phone());
        }

        if (request.nickname() != null) {
            user.setNickname(
                    request.nickname().trim()
            );
        }

        if (request.email() != null) {
            user.setEmail(
                    request.email().isBlank()
                            ? null
                            : request.email().trim()
            );
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return toProfile(user);
    }

    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request) {

        SysUser user = requireUser(userId);

        if (!passwordEncoder.matches(
                request.oldPassword(),
                user.getPasswordHash()
        )) {
            throw new BusinessException(
                    ErrorCode.OLD_PASSWORD_ERROR
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "新密码不能与原密码相同"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        refreshTokenMapper.revokeAllByUserId(
                userId,
                LocalDateTime.now()
        );
    }

    public UserProfileResponse toProfile(
            SysUser user) {

        return new UserProfileResponse(
                user.getId(),
                user.getPhone(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus(),
                userMapper.selectRoleCodesByUserId(
                        user.getId()
                ),
                user.getCreatedAt()
        );
    }

    private SysUser requireUser(Long userId) {

        SysUser user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "用户不存在"
            );
        }

        return user;
    }
}