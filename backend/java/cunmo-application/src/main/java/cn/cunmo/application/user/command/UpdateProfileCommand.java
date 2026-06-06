package cn.cunmo.application.user.command;

/**
 * 更新当前用户资料命令。
 *
 * @param userId 当前登录用户主键
 * @param nickname 用户昵称
 * @param avatarUrl 长期头像地址
 */
public record UpdateProfileCommand(
        long userId,
        String nickname,
        String avatarUrl) {
}
