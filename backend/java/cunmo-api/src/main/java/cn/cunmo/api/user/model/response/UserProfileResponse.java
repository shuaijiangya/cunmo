package cn.cunmo.api.user.model.response;

/**
 * 当前用户资料响应。
 *
 * @param id 用户主键
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param profileCompleted 资料是否完整
 */
public record UserProfileResponse(
        long id,
        String nickname,
        String avatarUrl,
        boolean profileCompleted) {
}
