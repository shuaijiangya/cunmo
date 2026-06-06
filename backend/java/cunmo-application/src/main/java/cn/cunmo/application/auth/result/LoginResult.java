package cn.cunmo.application.auth.result;

import java.util.List;

/**
 * 微信登录用例结果，不包含任何基础设施类型。
 */
public record LoginResult(
        String token,
        long expiresIn,
        long userId,
        String nickname,
        String avatarUrl,
        boolean profileCompleted,
        List<String> roles,
        List<String> permissions) {

    /**
     * 将角色和权限复制为不可变列表，防止结果被外部修改。
     */
    public LoginResult {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}
