package cn.cunmo.api.auth.model.response;

import java.util.List;

/**
 * 登录成功后的用户视图。
 */
public record LoginUserResponse(
        long id,
        String nickname,
        String avatarUrl,
        boolean profileCompleted,
        List<String> roles,
        List<String> permissions) {

    /**
     * 将角色和权限列表复制为不可变集合。
     */
    public LoginUserResponse {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}
