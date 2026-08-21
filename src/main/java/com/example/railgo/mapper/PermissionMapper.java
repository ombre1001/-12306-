package com.example.railgo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.Permission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    @Select("""
            SELECT p.permission_code
            FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
            ORDER BY p.permission_code
            """)
    List<String> selectCodesByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteRolePermissions(@Param("roleId") Long roleId);

    @Insert("""
            INSERT INTO role_permission(role_id, permission_id)
            SELECT #{roleId}, id FROM permission WHERE permission_code = #{permissionCode}
            """)
    int insertRolePermission(@Param("roleId") Long roleId,
                             @Param("permissionCode") String permissionCode);
}
