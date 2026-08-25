package com.example.railgo.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.railgo.data.dto.LoginRequest;
import com.example.railgo.data.dto.LogoutRequest;
import com.example.railgo.data.dto.RefreshTokenRequest;
import com.example.railgo.data.dto.RegisterRequest;
import com.example.railgo.data.dto.SendEmailCodeRequest;
import com.example.railgo.data.po.AuthRefreshToken;
import com.example.railgo.data.po.User;
import com.example.railgo.data.vo.AuthResponse;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.AuthRefreshTokenMapper;
import com.example.railgo.mapper.UserMapper;
import com.example.railgo.security.JwtTokenProvider;
import com.example.railgo.security.TokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE =
            "PASSENGER";

    private final UserMapper userMapper;

    private final AuthRefreshTokenMapper
            refreshTokenMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider tokenProvider;

    private final EmailVerificationCodeService
            emailVerificationCodeService;

    private final LoginAttemptService
            loginAttemptService;

    private final UserService userService;

    @Transactional
    public AuthResponse register(
            RegisterRequest request) {

        String email = emailVerificationCodeService.normalize(request.email());

        if (findByEmail(email) != null) {
            throw new BusinessException(
                    ErrorCode.EMAIL_EXISTS
            );
        }

        emailVerificationCodeService.verifyAndConsume(
                email,
                request.verificationCode()
        );

        LocalDateTime now = LocalDateTime.now();

        User user = new User();

        user.setEmail(email);

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.password()
                )
        );

        if (request.nickname() == null
                || request.nickname().isBlank()) {

            user.setNickname(
                    "用户"
                            + email.substring(
                                    0,
                                    Math.min(email.indexOf('@'), 8)
                            )
            );

        } else {
            user.setNickname(
                    request.nickname().trim()
            );
        }

        user.setStatus("ENABLED");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        int roleRows = userMapper.insertUserRole(
                user.getId(),
                DEFAULT_ROLE
        );

        if (roleRows != 1) {
            throw new BusinessException(
                    ErrorCode.DATABASE_ERROR,
                    "默认角色未初始化"
            );
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(
            LoginRequest request) {

        String account = normalizeAccount(request.account());

        loginAttemptService.checkAllowed(account);

        User user = findByAccount(
                account
        );

        if (user == null
                || !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {

            loginAttemptService.recordFailure(
                    account
            );

            throw new BusinessException(
                    ErrorCode.LOGIN_FAILED
            );
        }

        loginAttemptService.recordSuccess(
                account
        );

        return completeLogin(user);
    }

    public void sendEmailRegistrationCode(
            SendEmailCodeRequest request,
            String clientIp) {

        String email = emailVerificationCodeService.normalize(request.email());
        if (findByEmail(email) != null) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        emailVerificationCodeService.sendRegistrationCode(email, clientIp);
    }

    @Transactional
    public AuthResponse refresh(
            RefreshTokenRequest request) {

        TokenClaims claims =
                tokenProvider.parseRefreshToken(
                        request.refreshToken()
                );

        AuthRefreshToken stored =
                refreshTokenMapper.selectOne(
                        Wrappers
                                .<AuthRefreshToken>
                                        lambdaQuery()
                                .eq(
                                        AuthRefreshToken
                                                ::getTokenJti,
                                        claims.jti()
                                )
                                .eq(
                                        AuthRefreshToken
                                                ::getUserId,
                                        claims.userId()
                                )
                                .isNull(
                                        AuthRefreshToken
                                                ::getRevokedAt
                                )
                                .gt(
                                        AuthRefreshToken
                                                ::getExpiresAt,
                                        LocalDateTime.now()
                                )
                );

        if (stored == null) {
            throw new BusinessException(
                    ErrorCode.REFRESH_TOKEN_INVALID
            );
        }

        int updated = refreshTokenMapper.update(
                null,
                Wrappers
                        .<AuthRefreshToken>
                                lambdaUpdate()
                        .eq(
                                AuthRefreshToken::getId,
                                stored.getId()
                        )
                        .isNull(
                                AuthRefreshToken
                                        ::getRevokedAt
                        )
                        .set(
                                AuthRefreshToken
                                        ::getRevokedAt,
                                LocalDateTime.now()
                        )
        );

        if (updated != 1) {
            throw new BusinessException(
                    ErrorCode.REFRESH_TOKEN_INVALID
            );
        }

        User user = userMapper.selectById(
                claims.userId()
        );

        if (user == null
                || !"ENABLED".equals(
                user.getStatus()
        )) {

            throw new BusinessException(
                    ErrorCode.ACCOUNT_DISABLED
            );
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(
            Long currentUserId,
            LogoutRequest request) {

        if (request == null
                || request.refreshToken() == null
                || request.refreshToken().isBlank()) {

            refreshTokenMapper.revokeAllByUserId(
                    currentUserId,
                    LocalDateTime.now()
            );

            return;
        }

        TokenClaims claims =
                tokenProvider.parseRefreshToken(
                        request.refreshToken()
                );

        if (!currentUserId.equals(
                claims.userId()
        )) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN
            );
        }

        refreshTokenMapper.update(
                null,
                Wrappers
                        .<AuthRefreshToken>
                                lambdaUpdate()
                        .eq(
                                AuthRefreshToken
                                        ::getTokenJti,
                                claims.jti()
                        )
                        .eq(
                                AuthRefreshToken
                                        ::getUserId,
                                currentUserId
                        )
                        .isNull(
                                AuthRefreshToken
                                        ::getRevokedAt
                        )
                        .set(
                                AuthRefreshToken
                                        ::getRevokedAt,
                                LocalDateTime.now()
                        )
        );
    }

    private AuthResponse issueTokens(
            User user) {

        List<String> roles =
                userMapper
                        .selectRoleCodesByUserId(
                                user.getId()
                        );

        String accessToken =
                tokenProvider.createAccessToken(
                        user.getId(),
                        user.getPhone(),
                        roles
                );

        String refreshToken =
                tokenProvider.createRefreshToken(
                        user.getId(),
                        user.getPhone(),
                        roles
                );

        TokenClaims refreshClaims =
                tokenProvider.parseRefreshToken(
                        refreshToken
                );

        AuthRefreshToken record =
                new AuthRefreshToken();

        record.setUserId(user.getId());

        record.setTokenJti(
                refreshClaims.jti()
        );

        record.setExpiresAt(
                LocalDateTime.ofInstant(
                        refreshClaims.expiresAt(),
                        ZoneId.systemDefault()
                )
        );

        record.setCreatedAt(
                LocalDateTime.now()
        );

        refreshTokenMapper.insert(record);

        return new AuthResponse(
                "Bearer",
                accessToken,
                refreshToken,
                tokenProvider
                        .getAccessTokenSeconds(),
                userService.toProfile(user)
        );
    }

    private AuthResponse completeLogin(User user) {
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
        return issueTokens(user);
    }

    private User findByPhone(
            String phone) {

        return userMapper.selectOne(
                Wrappers
                        .<User>lambdaQuery()
                        .eq(
                                User::getPhone,
                                phone
                        )
        );
    }

    private User findByEmail(String email) {
        return userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getEmail, email)
        );
    }

    private User findByAccount(String rawAccount) {
        String account = normalizeAccount(rawAccount);
        if (account.contains("@")) {
            return findByEmail(
                    emailVerificationCodeService.normalize(account)
            );
        }
        return findByPhone(account);
    }

    private String normalizeAccount(String rawAccount) {
        String account = rawAccount.trim();
        return account.contains("@")
                ? emailVerificationCodeService.normalize(account)
                : account;
    }
}
