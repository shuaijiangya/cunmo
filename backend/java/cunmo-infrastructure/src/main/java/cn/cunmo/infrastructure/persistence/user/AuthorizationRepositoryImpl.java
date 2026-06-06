package cn.cunmo.infrastructure.persistence.user;

import cn.cunmo.domain.auth.model.valueobject.AuthorizationSnapshot;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.cunmo.domain.auth.repository.AuthorizationRepository;
import cn.cunmo.infrastructure.persistence.user.mapper.AuthorizationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * MySQL 用户授权查询实现。
 */
@Repository
public class AuthorizationRepositoryImpl implements AuthorizationRepository {
    private static final Logger log =
            LoggerFactory.getLogger(AuthorizationRepositoryImpl.class);

    private final AuthorizationMapper authorizationMapper;

    /**
     * 创建授权查询仓储。
     */
    public AuthorizationRepositoryImpl(AuthorizationMapper authorizationMapper) {
        this.authorizationMapper = authorizationMapper;
    }

    /**
     * 查询并构造用户当前有效授权快照。
     */
    @Override
    public AuthorizationSnapshot findActiveAuthorization(UserId userId) {
        log.debug(
                "event=wechat_login stage=authorization_query_started userId={}",
                userId.value());
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                authorizationMapper.selectActiveRoleCodes(userId.value()),
                authorizationMapper.selectActivePermissionCodes(userId.value()));
        log.debug(
                "event=wechat_login stage=authorization_query_completed userId={} roleCount={} permissionCount={}",
                userId.value(),
                snapshot.roleCodes().size(),
                snapshot.permissionCodes().size());
        return snapshot;
    }
}
