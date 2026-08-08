package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode}")
    Long selectIdByCode(@Param("roleCode") String roleCode);

    @Select("""
            SELECT COUNT(*)
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.status = 'ENABLED' AND r.role_code = 'SYSTEM_ADMIN'
            """)
    long countEnabledSystemAdmins();

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);
}
