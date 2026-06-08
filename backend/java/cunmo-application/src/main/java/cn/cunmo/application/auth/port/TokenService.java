package cn.cunmo.application.auth.port;

import cn.cunmo.domain.auth.model.valueobject.UserId;
import java.util.List;

/**
 * 业务登录态签发端口。
 */
public interface TokenService {

    /**
     * 为用户签发包含角色和权限上下文的业务登录态。
     *
     * @param userId 用户标识
     * @param roles 有效角色编码
     * @param permissions 有效权限编码
     * @return Token 值与剩余有效期
     */
    TokenResult issue(UserId userId, List<String> roles, List<String> permissions);

    /**
     * 注销当前请求携带的业务 Token，不影响同用户其他会话。
     */
    void logoutCurrentToken();

    /**
     * Token 签发结果。
     */
    record TokenResult(String token, long expiresIn) {
    }
}
