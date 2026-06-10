package cn.cunmo.application.auth.port;

/**
 * 当前登录用户权限校验端口。
 */
public interface CurrentPermissionProvider {
    void requirePermission(String permissionCode);
}
