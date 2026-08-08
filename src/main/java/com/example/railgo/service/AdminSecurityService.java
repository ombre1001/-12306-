package com.example.railgo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.railgo.data.dto.*;
import com.example.railgo.data.po.AuthRefreshToken;
import com.example.railgo.data.po.SysPermission;
import com.example.railgo.data.po.SysRole;
import com.example.railgo.data.po.SysUser;
import com.example.railgo.data.vo.admin.AdminUserResponse;
import com.example.railgo.data.vo.admin.PermissionResponse;
import com.example.railgo.data.vo.admin.RoleResponse;
import com.example.railgo.exception.BusinessException;
import com.example.railgo.exception.ErrorCode;
import com.example.railgo.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminSecurityService {
    private static final Set<String> ADMIN_ROLES = Set.of("BUSINESS_ADMIN", "SYSTEM_ADMIN");
    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final AuthRefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;

    public IPage<AdminUserResponse> pageUsers(long page, long size, String keyword,
                                               String status, String roleCode) {
        var query = Wrappers.<SysUser>lambdaQuery().orderByDesc(SysUser::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(q -> q.like(SysUser::getPhone, value)
                    .or().like(SysUser::getNickname, value)
                    .or().like(SysUser::getEmail, value));
        }
        if (StringUtils.hasText(status)) {
            String normalized = normalizeStatus(status);
            query.eq(SysUser::getStatus, normalized);
        }
        if (StringUtils.hasText(roleCode)) {
            Long roleId = requireRoleId(roleCode);
            query.inSql(SysUser::getId,
                    "SELECT user_id FROM sys_user_role WHERE role_id = " + roleId);
        }

        Page<SysUser> source = userMapper.selectPage(new Page<>(page, size), query);
        Page<AdminUserResponse> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toUserResponse).toList());
        return result;
    }

    public AdminUserResponse getUser(Long userId) {
        return toUserResponse(requireUser(userId));
    }

    @Transactional
    public AdminUserResponse createAdmin(AdminCreateRequest request) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhone, request.phone()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.ADMIN_PHONE_EXISTS);
        }
        validatePassword(request.password());
        Set<String> roles = normalizeRoles(request.roles());
        if (roles.stream().noneMatch(ADMIN_ROLES::contains)) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_INVALID, "新管理员至少需要一个管理员角色");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setEmail(trimToNull(request.email()));
        user.setStatus("ENABLED");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        if (userMapper.insert(user) != 1) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR);
        }
        insertRoles(user.getId(), roles);
        return toUserResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUpdateRequest request) {
        SysUser user = requireUser(userId);
        user.setNickname(request.nickname().trim());
        user.setEmail(trimToNull(request.email()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toUserResponse(user);
    }

    @Transactional
    public void updateStatus(Long operatorId, Long userId, AdminUserStatusRequest request) {
        SysUser user = requireUser(userId);
        String status = normalizeStatus(request.status());
        if (operatorId.equals(userId) && "DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_DISABLE_SELF);
        }
        if ("DISABLED".equals(status) && hasRole(userId, "SYSTEM_ADMIN")) {
            requireAnotherSystemAdmin();
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        if ("DISABLED".equals(status)) {
            revokeRefreshTokens(userId);
        }
    }

    @Transactional
    public void replaceRoles(Long operatorId, Long userId, AdminUserRolesRequest request) {
        SysUser user = requireUser(userId);
        Set<String> roles = normalizeRoles(request.roles());
        boolean removingSystemAdmin = hasRole(userId, "SYSTEM_ADMIN") && !roles.contains("SYSTEM_ADMIN");
        if (operatorId.equals(userId) && removingSystemAdmin) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_INVALID, "不能移除当前账号的系统管理员角色");
        }
        if ("ENABLED".equals(user.getStatus()) && removingSystemAdmin) {
            requireAnotherSystemAdmin();
        }
        roleMapper.deleteUserRoles(userId);
        insertRoles(userId, roles);
        revokeRefreshTokens(userId);
    }

    @Transactional
    public void resetPassword(Long userId, AdminResetPasswordRequest request) {
        SysUser user = requireUser(userId);
        validatePassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        revokeRefreshTokens(userId);
    }

    public List<RoleResponse> listRoles() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .orderByAsc(SysRole::getId))
                .stream().map(role -> {
                    RoleResponse response = new RoleResponse();
                    response.setId(role.getId());
                    response.setRoleCode(role.getRoleCode());
                    response.setRoleName(role.getRoleName());
                    response.setDescription(null);
                    response.setPermissions(permissionMapper.selectCodesByRoleId(role.getId()));
                    return response;
                }).toList();
    }

    public List<PermissionResponse> listPermissions(String module) {
        var query = Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getStatus, "ENABLED")
                .orderByAsc(SysPermission::getModule, SysPermission::getPermissionCode);
        if (StringUtils.hasText(module)) {
            query.eq(SysPermission::getModule, module.trim().toUpperCase(Locale.ROOT));
        }
        return permissionMapper.selectList(query).stream().map(permission -> {
            PermissionResponse response = new PermissionResponse();
            response.setId(permission.getId());
            response.setPermissionCode(permission.getPermissionCode());
            response.setPermissionName(permission.getPermissionName());
            response.setModule(permission.getModule());
            response.setDescription(permission.getDescription());
            response.setStatus(permission.getStatus());
            return response;
        }).toList();
    }

    @Transactional
    public void replaceRolePermissions(String roleCode, RolePermissionsRequest request) {
        Long roleId = requireRoleId(roleCode);
        Set<String> codes = request.permissions().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!codes.isEmpty()) {
            Long existing = permissionMapper.selectCount(Wrappers.<SysPermission>lambdaQuery()
                    .in(SysPermission::getPermissionCode, codes)
                    .eq(SysPermission::getStatus, "ENABLED"));
            if (existing != codes.size()) {
                throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
            }
        }
        permissionMapper.deleteRolePermissions(roleId);
        codes.forEach(code -> permissionMapper.insertRolePermission(roleId, code));
    }

    private AdminUserResponse toUserResponse(SysUser user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setPhone(user.getPhone());
        response.setNickname(user.getNickname());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus());
        response.setRoles(userMapper.selectRoleCodesByUserId(user.getId()));
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
        return user;
    }

    private Long requireRoleId(String roleCode) {
        if (!StringUtils.hasText(roleCode)) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        Long id = roleMapper.selectIdByCode(roleCode.trim().toUpperCase(Locale.ROOT));
        if (id == null) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        return id;
    }

    private Set<String> normalizeRoles(Set<String> requested) {
        Set<String> roles = requested.stream().filter(StringUtils::hasText)
                .map(String::trim).map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (roles.isEmpty()) throw new BusinessException(ErrorCode.ADMIN_ROLE_INVALID);
        roles.forEach(this::requireRoleId);
        return roles;
    }

    private void insertRoles(Long userId, Set<String> roles) {
        for (String role : roles) {
            if (userMapper.insertUserRole(userId, role) != 1) {
                throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
            }
        }
    }

    private boolean hasRole(Long userId, String role) {
        return userMapper.selectRoleCodesByUserId(userId).contains(role);
    }

    private void requireAnotherSystemAdmin() {
        if (roleMapper.countEnabledSystemAdmins() <= 1) {
            throw new BusinessException(ErrorCode.LAST_SYSTEM_ADMIN);
        }
    }

    private void revokeRefreshTokens(Long userId) {
        refreshTokenMapper.update(null, Wrappers.<AuthRefreshToken>lambdaUpdate()
                .eq(AuthRefreshToken::getUserId, userId)
                .isNull(AuthRefreshToken::getRevokedAt)
                .set(AuthRefreshToken::getRevokedAt, LocalDateTime.now()));
    }

    private String normalizeStatus(String value) {
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) throw new BusinessException(ErrorCode.ADMIN_STATUS_INVALID);
        return status;
    }

    private void validatePassword(String password) {
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$")) {
            throw new BusinessException(ErrorCode.ADMIN_PASSWORD_INVALID,
                    "密码至少8位，且必须包含大写字母、小写字母和数字");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
