package cn.cunmo.domain.auth.repository;

import cn.cunmo.domain.auth.model.aggregate.User;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;
import java.time.Instant;

/**
 * 用户聚合持久化端口。
 */
public interface UserRepository {

    /**
     * 按微信身份读取用户；身份不存在时原子创建用户并绑定默认角色。
     *
     * @param principal 微信身份
     * @param defaultRoleCode 新用户默认角色编码
     * @return 已存在或新创建的用户聚合
     */
    User findOrRegisterWechatUser(WechatPrincipal principal, String defaultRoleCode);

    /**
     * 记录用户最近一次成功登录时间。
     *
     * @param userId 用户标识
     * @param loginTime 登录时间
     */
    void recordSuccessfulLogin(UserId userId, Instant loginTime);

    /**
     * 更新用户主动授权填写的昵称与头像。
     *
     * @param userId 用户标识
     * @param nickname 用户昵称
     * @param avatarUrl 长期头像地址
     * @return 更新后的用户聚合
     */
    User updateProfile(
            UserId userId,
            String nickname,
            String avatarUrl);
}
