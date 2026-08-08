package com.example.railgo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.railgo.data.dto.*;
import com.example.railgo.data.vo.Result;
import com.example.railgo.data.vo.admin.AdminUserResponse;
import com.example.railgo.data.vo.admin.PermissionResponse;
import com.example.railgo.data.vo.admin.RoleResponse;
import com.example.railgo.security.RailUserPrincipal;
import com.example.railgo.service.AdminSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理员与权限接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS_ADMIN','SYSTEM_ADMIN')")
public class AdminSecurityController {
    private final AdminSecurityService service;

    @Operation(summary = "分页查询用户和管理员")
    @GetMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:USER:READ')")
    public ResponseEntity<Result<IPage<AdminUserResponse>>> users(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode) {
        return Result.success(service.pageUsers(page, size, keyword, status, roleCode));
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:USER:READ')")
    public ResponseEntity<Result<AdminUserResponse>> user(@PathVariable @Positive Long userId) {
        return Result.success(service.getUser(userId));
    }

    @Operation(summary = "创建管理员")
    @PostMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:USER:WRITE')")
    public ResponseEntity<Result<AdminUserResponse>> create(@Valid @RequestBody AdminCreateRequest request) {
        return Result.success(service.createAdmin(request), "管理员创建成功");
    }

    @Operation(summary = "修改用户基本资料")
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:USER:WRITE')")
    public ResponseEntity<Result<AdminUserResponse>> update(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AdminUpdateRequest request) {
        return Result.success(service.updateUser(userId, request), "用户资料修改成功");
    }

    @Operation(summary = "启用或禁用用户")
    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:USER:WRITE')")
    public ResponseEntity<Result<Void>> status(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AdminUserStatusRequest request) {
        service.updateStatus(principal.userId(), userId, request);
        return Result.ok();
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:ROLE:WRITE')")
    public ResponseEntity<Result<Void>> roles(
            @AuthenticationPrincipal RailUserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AdminUserRolesRequest request) {
        service.replaceRoles(principal.userId(), userId, request);
        return Result.ok();
    }

    @Operation(summary = "管理员重置用户密码")
    @PutMapping("/users/{userId}/password")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:USER:WRITE')")
    public ResponseEntity<Result<Void>> password(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        service.resetPassword(userId, request);
        return Result.ok();
    }

    @Operation(summary = "查询角色及其权限")
    @GetMapping("/roles")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:ROLE:READ')")
    public ResponseEntity<Result<List<RoleResponse>>> roles() {
        return Result.success(service.listRoles());
    }

    @Operation(summary = "查询权限字典")
    @GetMapping("/permissions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:ROLE:READ')")
    public ResponseEntity<Result<List<PermissionResponse>>> permissions(
            @RequestParam(required = false) String module) {
        return Result.success(service.listPermissions(module));
    }

    @Operation(summary = "替换角色权限")
    @PutMapping("/roles/{roleCode}/permissions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('ADMIN:ROLE:WRITE')")
    public ResponseEntity<Result<Void>> rolePermissions(
            @PathVariable String roleCode,
            @Valid @RequestBody RolePermissionsRequest request) {
        service.replaceRolePermissions(roleCode, request);
        return Result.ok();
    }
}
