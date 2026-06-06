package cn.cunmo.trigger.http.converter;

import cn.cunmo.api.auth.model.response.LoginResponse;
import cn.cunmo.api.auth.model.response.LoginUserResponse;
import cn.cunmo.application.auth.result.LoginResult;

/**
 * 登录用例结果与 HTTP 响应 DTO 转换器。
 */
public final class AuthHttpConverter {
    /**
     * 禁止实例化无状态 HTTP 转换器。
     */
    private AuthHttpConverter() {
    }

    /**
     * 将应用登录结果转换为公开 HTTP 响应。
     */
    public static LoginResponse toResponse(LoginResult result) {
        return new LoginResponse(
                result.token(),
                result.expiresIn(),
                new LoginUserResponse(
                        result.userId(),
                        result.nickname(),
                        result.avatarUrl(),
                        result.profileCompleted(),
                        result.roles(),
                        result.permissions()));
    }
}
