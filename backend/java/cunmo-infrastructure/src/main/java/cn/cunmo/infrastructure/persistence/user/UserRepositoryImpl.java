package cn.cunmo.infrastructure.persistence.user;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.auth.model.aggregate.User;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;
import cn.cunmo.domain.auth.repository.UserRepository;
import cn.cunmo.infrastructure.persistence.user.converter.UserPersistenceConverter;
import cn.cunmo.infrastructure.persistence.user.dataobject.UserDO;
import cn.cunmo.infrastructure.persistence.user.dataobject.WechatIdentityDO;
import cn.cunmo.infrastructure.persistence.user.mapper.AuthorizationMapper;
import cn.cunmo.infrastructure.persistence.user.mapper.UserMapper;
import cn.cunmo.infrastructure.persistence.user.mapper.WechatIdentityMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MySQL 用户仓储实现。
 *
 * <p>首次注册在独立事务内创建用户、微信身份和默认角色。并发重复身份会回滚
 * 当前事务并重新读取已成功创建的用户。</p>
 */
@Repository
public class UserRepositoryImpl implements UserRepository {
    private static final Logger log =
            LoggerFactory.getLogger(UserRepositoryImpl.class);

    private final UserMapper userMapper;
    private final WechatIdentityMapper identityMapper;
    private final AuthorizationMapper authorizationMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建用户仓储并建立事务模板。
     */
    public UserRepositoryImpl(
            UserMapper userMapper,
            WechatIdentityMapper identityMapper,
            AuthorizationMapper authorizationMapper,
            PlatformTransactionManager transactionManager) {
        this.userMapper = userMapper;
        this.identityMapper = identityMapper;
        this.authorizationMapper = authorizationMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 查找微信用户；不存在时在事务内创建用户、身份并绑定默认角色。
     */
    @Override
    public User findOrRegisterWechatUser(
            WechatPrincipal principal,
            String defaultRoleCode) {
        log.debug("event=wechat_login stage=user_lookup_started");
        UserDO existing = find(principal);
        if (existing != null) {
            log.debug(
                    "event=wechat_login stage=user_lookup_hit userId={}",
                    existing.getId());
            return UserPersistenceConverter.toDomain(existing);
        }

        log.info("event=wechat_login stage=user_registration_started");
        try {
            transactionTemplate.executeWithoutResult(status ->
                    register(principal, defaultRoleCode));
        } catch (DuplicateKeyException concurrentRegistration) {
            log.warn(
                    "event=wechat_login stage=concurrent_registration_detected action=reload");
            UserDO concurrentUser = find(principal);
            if (concurrentUser != null) {
                log.info(
                        "event=wechat_login stage=concurrent_registration_recovered userId={}",
                        concurrentUser.getId());
                return UserPersistenceConverter.toDomain(concurrentUser);
            }
            log.error(
                    "event=wechat_login stage=concurrent_registration_failed errorCode=DATABASE_ERROR exceptionType={}",
                    concurrentRegistration.getClass().getSimpleName());
            throw new ApplicationException(
                    "DATABASE_ERROR",
                    "用户注册冲突，请稍后重试",
                    concurrentRegistration);
        }

        UserDO registered = find(principal);
        if (registered == null) {
            log.error(
                    "event=wechat_login stage=user_registration_reload_failed errorCode=DATABASE_ERROR");
            throw new ApplicationException("DATABASE_ERROR", "用户注册结果读取失败");
        }
        log.info(
                "event=wechat_login stage=user_registration_completed userId={}",
                registered.getId());
        return UserPersistenceConverter.toDomain(registered);
    }

    /**
     * 持久化用户最近成功登录时间。
     */
    @Override
    public void recordSuccessfulLogin(UserId userId, Instant loginTime) {
        userMapper.updateLastLogin(userId.value(), loginTime);
        log.debug(
                "event=wechat_login stage=last_login_persisted userId={}",
                userId.value());
    }

    /**
     * 更新用户昵称和头像并重新加载聚合。
     */
    @Override
    public User updateProfile(
            UserId userId,
            String nickname,
            String avatarUrl) {
        int affected = userMapper.updateProfile(
                userId.value(),
                nickname,
                avatarUrl);
        if (affected != 1) {
            throw new ApplicationException(
                    "USER_NOT_FOUND",
                    "当前用户不存在");
        }
        UserDO updated = userMapper.selectById(userId.value());
        if (updated == null) {
            throw new ApplicationException(
                    "USER_NOT_FOUND",
                    "当前用户不存在");
        }
        log.info(
                "event=user_profile stage=profile_persisted userId={}",
                userId.value());
        return UserPersistenceConverter.toDomain(updated);
    }

    /**
     * 按微信身份查询关联用户。
     */
    private UserDO find(WechatPrincipal principal) {
        return identityMapper.selectUserByPrincipal(
                principal.appId(),
                principal.openId());
    }

    /**
     * 在当前事务内完成新用户、微信身份和默认角色关系创建。
     */
    private void register(WechatPrincipal principal, String defaultRoleCode) {
        Long roleId = authorizationMapper.selectActiveRoleId(defaultRoleCode);
        if (roleId == null) {
            log.error(
                    "event=wechat_login stage=default_role_lookup_failed roleCode={} errorCode=DEFAULT_ROLE_MISSING",
                    defaultRoleCode);
            throw new ApplicationException(
                    "DEFAULT_ROLE_MISSING",
                    "系统默认角色未配置");
        }

        UserDO user = new UserDO();
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);

        WechatIdentityDO identity = new WechatIdentityDO();
        identity.setUserId(user.getId());
        identity.setAppid(principal.appId());
        identity.setOpenid(principal.openId());
        identity.setUnionid(principal.unionId());
        identityMapper.insert(identity);
        authorizationMapper.bindUserRole(user.getId(), roleId);
        log.debug(
                "event=wechat_login stage=default_role_bound userId={} roleCode={}",
                user.getId(),
                defaultRoleCode);
    }
}
