package com.example.railgo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT r.role_code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<String> selectRoleCodesByUserId(
            @Param("userId") Long userId
    );

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.status = 'ENABLED'
            ORDER BY p.permission_code
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO sys_user_role(user_id, role_id)
            SELECT #{userId}, id
            FROM sys_role
            WHERE role_code = #{roleCode}
            """)
    int insertUserRole(
            @Param("userId") Long userId,
            @Param("roleCode") String roleCode
    );
}
