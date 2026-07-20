package com.example.railgo.controller;


import com.example.railgo.data.dto.LoginRequest;
import com.example.railgo.data.dto.LogoutRequest;
import com.example.railgo.data.dto.RefreshTokenRequest;
import com.example.railgo.data.dto.RegisterRequest;
import com.example.railgo.service.AuthService;
import com.example.railgo.data.vo.AuthResponse;
import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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