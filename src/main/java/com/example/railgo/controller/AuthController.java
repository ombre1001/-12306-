package com.example.railgo.controller;


import com.example.railgo.data.dto.LoginRequest;
import com.example.railgo.data.dto.LogoutRequest;
import com.example.railgo.data.dto.RefreshTokenRequest;
import com.example.railgo.data.dto.RegisterRequest;
import com.example.railgo.service.AuthService;
import com.example.railgo.data.vo.AuthResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "认证接口",
        description = "用户注册、登录、刷新令牌和退出登录"
)
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "用户注册",
            description = "使用手机号、密码和验证码注册，注册成功后返回访问令牌和刷新令牌"
    )
    @PostMapping("/register")
    public ResponseEntity<Result<AuthResponse>>
    register(
            @Valid
            @RequestBody
            RegisterRequest request) {

        Result<AuthResponse> result =
                new Result<>(
                        0,
                        authService.register(request),
                        "注册成功"
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    @Operation(
            summary = "用户登录",
            description = "使用手机号和密码登录"
    )
    @PostMapping("/login")
    public ResponseEntity<Result<AuthResponse>>
    login(
            @Valid
            @RequestBody
            LoginRequest request) {

        return Result.success(
                authService.login(request)
        );
    }

    @Operation(
            summary = "刷新令牌",
            description = "使用 refreshToken 获取新的 accessToken 和 refreshToken"
    )
    @PostMapping("/refresh")
    public ResponseEntity<Result<AuthResponse>>
    refresh(
            @Valid
            @RequestBody
            RefreshTokenRequest request) {

        return Result.success(
                authService.refresh(request)
        );
    }

    @Operation(
            summary = "退出登录",
            description = "撤销当前刷新令牌；不传 refreshToken 时撤销该用户的全部刷新令牌"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Result<Void>>
    logout(
            @AuthenticationPrincipal
            RailUserPrincipal principal,

            @RequestBody(required = false)
            LogoutRequest request) {

        authService.logout(
                principal.userId(),
                request
        );

        return Result.ok();
    }
}