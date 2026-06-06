package cn.cunmo.infrastructure.security.satoken;

import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.exception.ApplicationException;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * 基于 Sa-Token 的当前用户读取适配器。
 */
@Component
public class SaTokenCurrentUserProvider implements CurrentUserProvider {

    /**
     * 校验登录态并返回当前用户主键。
     */
    @Override
    public long requireUserId() {
        if (!StpUtil.isLogin()) {
            throw new ApplicationException(
                    "UNAUTHORIZED",
                    "登录状态已失效，请重新登录");
        }
        return StpUtil.getLoginIdAsLong();
    }
}
