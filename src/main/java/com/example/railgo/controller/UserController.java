package com.example.railgo.controller;


import com.example.railgo.data.vo.Result;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.data.dto.ChangePasswordRequest;
import com.example.railgo.data.dto.UpdateProfileRequest;
import com.example.railgo.service.UserService;
import com.example.railgo.data.vo.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
