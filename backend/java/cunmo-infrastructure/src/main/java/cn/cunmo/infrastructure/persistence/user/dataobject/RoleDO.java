package cn.cunmo.infrastructure.persistence.user.dataobject;

/**
 * sys_role 数据库映射对象。
 */
public class RoleDO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String dataScope;
    private Integer status;

    /** 返回角色主键。 */
    public Long getId() {
        return id;
    }

    /** 设置角色主键。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 返回角色唯一编码。 */
    public String getRoleCode() {
        return roleCode;
    }

    /** 设置角色唯一编码。 */
    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    /** 返回角色显示名称。 */
    public String getRoleName() {
        return roleName;
    }

    /** 设置角色显示名称。 */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /** 返回角色数据权限范围。 */
    public String getDataScope() {
        return dataScope;
    }

    /** 设置角色数据权限范围。 */
    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    /** 返回角色状态数据库值。 */
    public Integer getStatus() {
        return status;
    }

    /** 设置角色状态数据库值。 */
    public void setStatus(Integer status) {
        this.status = status;
    }
}
