package cn.cunmo.infrastructure.persistence.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户角色与权限关系 Mapper。
 */
public interface AuthorizationMapper {

    /**
     * 按角色编码查询有效角色主键。
     */
    @Select("""
            SELECT id
            FROM sys_role
            WHERE role_code = #{roleCode}
              AND status = 1
              AND deleted = 0
            LIMIT 1
            """)
    Long selectActiveRoleId(@Param("roleCode") String roleCode);

    /**
     * 幂等绑定用户与角色关系。
     */
    @Insert("""
            INSERT IGNORE INTO sys_user_role (user_id, role_id, created_at)
            VALUES (#{userId}, #{roleId}, CURRENT_TIMESTAMP(3))
            """)
    int bindUserRole(
            @Param("userId") long userId,
            @Param("roleId") long roleId);

    /**
     * 查询用户全部有效角色编码。
     */
    @Select("""
            SELECT DISTINCT role_table.role_code
            FROM sys_user_role relation
            JOIN sys_role role_table ON role_table.id = relation.role_id
            WHERE relation.user_id = #{userId}
              AND role_table.status = 1
              AND role_table.deleted = 0
            ORDER BY role_table.role_code
            """)
    List<String> selectActiveRoleCodes(@Param("userId") long userId);

    /**
     * 查询用户通过角色获得的全部有效权限编码。
     */
    @Select("""
            SELECT DISTINCT permission_table.permission_code
            FROM sys_user_role user_role
            JOIN sys_role role_table ON role_table.id = user_role.role_id
            JOIN sys_role_permission role_permission
              ON role_permission.role_id = role_table.id
            JOIN sys_permission permission_table
              ON permission_table.id = role_permission.permission_id
            WHERE user_role.user_id = #{userId}
              AND role_table.status = 1
              AND role_table.deleted = 0
              AND permission_table.status = 1
              AND permission_table.deleted = 0
            ORDER BY permission_table.permission_code
            """)
    List<String> selectActivePermissionCodes(@Param("userId") long userId);
}
