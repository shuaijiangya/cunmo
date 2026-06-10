package cn.cunmo.infrastructure.security.satoken;

import cn.cunmo.application.auth.port.CurrentPermissionProvider;
import cn.cunmo.application.exception.ApplicationException;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

@Component
public class SaTokenCurrentPermissionProvider
        implements CurrentPermissionProvider {
    @Override
    public void requirePermission(String permissionCode) {
        if (!StpUtil.hasPermission(permissionCode)) {
            throw new ApplicationException(
                    "FORBIDDEN",
                    "当前账号无权执行此操作");
        }
    }
}
