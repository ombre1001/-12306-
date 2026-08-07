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
import com.example.railgo.data.dto.CreatePassengerRequest;
import com.example.railgo.data.dto.UpdatePassengerRequest;
import com.example.railgo.data.vo.PassengerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Tag(
        name = "用户接口",
        description = "个人资料、密码和常用乘车人管理"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users/me")
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

    @Operation(summary = "查询当前用户的常用乘车人")
    @GetMapping("/passengers")
    public ResponseEntity<
            Result<List<PassengerResponse>>>
    getPassengers(
            @AuthenticationPrincipal
            RailUserPrincipal principal) {

        return Result.success(
                userService.getPassengers(
                        principal.userId()
                )
        );
    }

    @Operation(summary = "查询常用乘车人详情")
    @GetMapping("/passengers/{id}")
    public ResponseEntity<
            Result<PassengerResponse>>
    getPassenger(
            @AuthenticationPrincipal
            RailUserPrincipal principal,

            @PathVariable
            @Positive(
                    message = "乘车人ID必须为正整数"
            )
            Long id) {

        return Result.success(
                userService.getPassenger(
                        principal.userId(),
                        id
                )
        );
    }

    @Operation(summary = "新增常用乘车人")
    @PostMapping("/passengers")
    public ResponseEntity<
            Result<PassengerResponse>>
    createPassenger(
            @AuthenticationPrincipal
            RailUserPrincipal principal,

            @Valid
            @RequestBody
            CreatePassengerRequest request) {

        return Result.success(
                userService.createPassenger(
                        principal.userId(),
                        request
                ),
                "乘车人添加成功"
        );
    }

    @Operation(summary = "修改常用乘车人")
    @PutMapping("/passengers/{id}")
    public ResponseEntity<
            Result<PassengerResponse>>
    updatePassenger(
            @AuthenticationPrincipal
            RailUserPrincipal principal,

            @PathVariable
            @Positive(
                    message = "乘车人ID必须为正整数"
            )
            Long id,

            @Valid
            @RequestBody
            UpdatePassengerRequest request) {

        return Result.success(
                userService.updatePassenger(
                        principal.userId(),
                        id,
                        request
                ),
                "乘车人修改成功"
        );
    }

    @Operation(summary = "删除常用乘车人")
    @DeleteMapping("/passengers/{id}")
    public ResponseEntity<Result<Void>>
    deletePassenger(
            @AuthenticationPrincipal
            RailUserPrincipal principal,

            @PathVariable
            @Positive(
                    message = "乘车人ID必须为正整数"
            )
            Long id) {

        userService.deletePassenger(
                principal.userId(),
                id
        );

        return Result.ok();
    }

}
