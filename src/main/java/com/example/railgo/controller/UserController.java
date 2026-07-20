package com.example.railgo.controller;


import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.data.dto.ChangePasswordRequest;
import com.example.railgo.data.dto.UpdateProfileRequest;
import com.example.railgo.service.UserService;
import com.example.railgo.data.vo.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "当前用户接口",
        description = "查询和修改当前登录用户资料"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @Operation(
            summary = "获取个人资料",
            description = "获取当前登录用户的手机号、昵称、邮箱、状态和角色"
    )
    @GetMapping
    public ResponseEntity<
            Result<UserProfileResponse>>
    getProfile(
            @AuthenticationPrincipal
            RailUserPrincipal principal) {

        return Result.success(
                userService.getProfile(
                        principal.userId()
                )
        );
    }

    @Operation(
            summary = "修改个人资料",
            description = "修改昵称、邮箱或手机号；修改手机号时必须提供验证码"
    )
    @PutMapping
    public ResponseEntity<
            Result<UserProfileResponse>>
    updateProfile(
            @AuthenticationPrincipal
            RailUserPrincipal principal,

            @Valid
            @RequestBody
            UpdateProfileRequest request) {

        return Result.success(
                userService.updateProfile(
                        principal.userId(),
                        request
                ),
                "资料修改成功"
        );
    }


    @Operation(
            summary = "修改密码",
            description = "校验原密码并设置新密码，成功后撤销全部刷新令牌"
    )
    @PutMapping("/password")
    public ResponseEntity<Result<Void>>
    changePassword(
            @AuthenticationPrincipal
            RailUserPrincipal principal,

            @Valid
            @RequestBody
            ChangePasswordRequest request) {

        userService.changePassword(
                principal.userId(),
                request
        );

        return Result.ok();
    }
}
