package cn.cunmo.api.auth.model.response;

/**
 * 微信登录成功响应。
 */
public record LoginResponse(
        String token,
        long expiresIn,
        LoginUserResponse user) {
}
