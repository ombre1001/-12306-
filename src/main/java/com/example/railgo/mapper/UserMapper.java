package com.example.railgo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.railgo.data.po.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT r.role_code
            FROM role r
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<String> selectRoleCodesByUserId(
            @Param("userId") Long userId
    );

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            JOIN user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.status = 'ENABLED'
            ORDER BY p.permission_code
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO user_role(user_id, role_id)
            SELECT #{userId}, id
            FROM role
            WHERE role_code = #{roleCode}
            """)
    int insertUserRole(
            @Param("userId") Long userId,
            @Param("roleCode") String roleCode
    );
}
