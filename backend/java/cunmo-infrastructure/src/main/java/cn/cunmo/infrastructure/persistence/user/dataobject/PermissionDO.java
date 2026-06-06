package cn.cunmo.infrastructure.persistence.user.dataobject;

/**
 * sys_permission 数据库映射对象。
 */
public class PermissionDO {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Integer status;

    /** 返回权限主键。 */
    public Long getId() {
        return id;
    }

    /** 设置权限主键。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 返回权限唯一编码。 */
    public String getPermissionCode() {
        return permissionCode;
    }

    /** 设置权限唯一编码。 */
    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    /** 返回权限显示名称。 */
    public String getPermissionName() {
        return permissionName;
    }

    /** 设置权限显示名称。 */
    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    /** 返回权限资源类型。 */
    public String getPermissionType() {
        return permissionType;
    }

    /** 设置权限资源类型。 */
    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    /** 返回权限状态数据库值。 */
    public Integer getStatus() {
        return status;
    }

    /** 设置权限状态数据库值。 */
    public void setStatus(Integer status) {
        this.status = status;
    }
}
