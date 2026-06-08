package cn.cunmo.infrastructure.security.satoken;

import cn.cunmo.application.auth.port.TokenService;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 业务登录态适配器。
 */
@Component
public class SaTokenService implements TokenService {
    private static final Logger log =
            LoggerFactory.getLogger(SaTokenService.class);

    private static final String SESSION_ROLES = "roles";
    private static final String SESSION_PERMISSIONS = "permissions";

    private final Runnable currentTokenLogout;

    /**
     * 创建使用 Sa-Token 当前会话注销能力的适配器。
     */
    public SaTokenService() {
        this(StpUtil::logout);
    }

    /**
     * 创建可替换当前 Token 注销操作的测试实例。
     *
     * @param currentTokenLogout 当前 Token 注销操作
     */
    SaTokenService(Runnable currentTokenLogout) {
        this.currentTokenLogout = currentTokenLogout;
    }

    /**
     * 使用 Sa-Token 建立登录态，并保存角色和权限会话快照。
     *
     * @param userId 用户标识
     * @param roles 角色编码列表
     * @param permissions 权限编码列表
     * @return 签发的 Token 及有效期
     */
    @Override
    public TokenResult issue(
            UserId userId,
            List<String> roles,
            List<String> permissions) {
        log.debug(
                "event=wechat_login stage=token_issue_started userId={} roleCount={} permissionCount={}",
                userId.value(),
                roles.size(),
                permissions.size());
        StpUtil.login(userId.value(), new SaLoginModel().setIsLastingCookie(false));
        StpUtil.getTokenSession().set(SESSION_ROLES, List.copyOf(roles));
        StpUtil.getTokenSession().set(
                SESSION_PERMISSIONS,
                List.copyOf(permissions));
        TokenResult result = new TokenResult(
                StpUtil.getTokenValue(),
                StpUtil.getTokenTimeout());
        log.debug(
                "event=wechat_login stage=token_issue_completed userId={} expiresIn={}",
                userId.value(),
                result.expiresIn());
        return result;
    }

    /**
     * 仅注销当前请求携带的 Token。
     */
    @Override
    public void logoutCurrentToken() {
        currentTokenLogout.run();
    }
}
