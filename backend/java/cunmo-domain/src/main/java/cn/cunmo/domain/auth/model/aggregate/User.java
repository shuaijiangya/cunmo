package cn.cunmo.domain.auth.model.aggregate;

import cn.cunmo.domain.auth.exception.DomainException;
import cn.cunmo.domain.auth.model.enums.UserStatus;
import cn.cunmo.domain.auth.model.valueobject.UserId;

/**
 * 用户聚合根，集中维护用户状态和资料完整度规则。
 */
public final class User {
    private final UserId id;
    private final String nickname;
    private final String avatarUrl;
    private final UserStatus status;

    /**
     * 通过持久化数据重建用户聚合。
     */
    private User(UserId id, String nickname, String avatarUrl, UserStatus status) {
        this.id = id;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.status = status;
    }

    /**
     * 创建尚未分配主键的微信新用户。
     *
     * @return 默认启用的新用户
     */
    public static User createWechatUser() {
        return new User(UserId.unassigned(), null, null, UserStatus.ENABLED);
    }

    /**
     * 从持久化状态还原用户聚合。
     *
     * @param id 用户标识
     * @param nickname 用户昵称
     * @param avatarUrl 头像地址
     * @param status 用户状态
     * @return 还原后的用户聚合
     */
    public static User reconstitute(
            UserId id,
            String nickname,
            String avatarUrl,
            UserStatus status) {
        return new User(id, nickname, avatarUrl, status);
    }

    /**
     * 校验当前用户是否允许登录。
     */
    public void assertCanLogin() {
        if (status != UserStatus.ENABLED) {
            throw new DomainException("USER_DISABLED", "当前账号已停用");
        }
    }

    /**
     * 判断用户是否已填写昵称和头像。
     *
     * @return 两项资料均存在时返回 true
     */
    public boolean profileCompleted() {
        return nickname != null && !nickname.isBlank()
                && avatarUrl != null && !avatarUrl.isBlank();
    }

    /**
     * 返回用户聚合标识。
     */
    public UserId id() {
        return id;
    }

    /**
     * 返回用户昵称。
     */
    public String nickname() {
        return nickname;
    }

    /**
     * 返回用户头像地址。
     */
    public String avatarUrl() {
        return avatarUrl;
    }

    /**
     * 返回用户当前状态。
     */
    public UserStatus status() {
        return status;
    }
}
