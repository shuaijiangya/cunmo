package cn.cunmo.application.user.result;

/**
 * 用户资料应用结果。
 *
 * @param userId 用户主键
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param profileCompleted 资料是否完整
 */
public record UserProfileResult(
        long userId,
        String nickname,
        String avatarUrl,
        boolean profileCompleted) {
}
